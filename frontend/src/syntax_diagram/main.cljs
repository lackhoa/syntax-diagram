(ns syntax-diagram.main
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [cljs.reader :refer [read-string]]
   [clojure.string :as str]
   [cljs.pprint :refer [cl-format]]
   [react-loading-animation :as loading]))

;; Link to the backend, which will then be relayed to Google API
(defonce backend-api "https://syntax-diagram-backend.herokuapp.com/")

;; Holds the data of the submitted text's syntax
;; Besides API info, each tokens is associated a `depth`, a `selected?` flag, and a `highlighted?` flag
(defonce state (r/atom {:sentences [] :tokens [] :newlines []}))
(def dark-theme? (r/atom false))

(defn enumerate [ls]
  (for [[x i] (map vector ls (range))] [x i]))

(defn head-token-id [token]
  (-> token :dependencyEdge :headTokenIndex))
(defn begin-offset [token]
  (-> token :text :beginOffset))
(defn content [token]
  (-> token :text :content))

(defn depth [id]
  (let [token ((:tokens @state) id)]
    (or (:depth token)
        (let [head-id (head-token-id token)
              dep (cond
                    ;; The root case
                    (= id head-id) 0
                    :else (inc (depth head-id)))]
          ;; Memoize this value for later
          (swap! state assoc-in [:tokens id :depth] dep)
          ;; Don't forget to return the depth!
          dep))))

(defn recalculate [input-text response]
  ;; Store the newline indices
  (-> response
      ;; Store newline indices
      (assoc  :newlines (for [[char i] (enumerate input-text)
                              :when (= char \newline)]
                          i))
      ;; Associate `selected?` and `highlighted?` and `id`
      (assoc :tokens (->> (for [[token id] (enumerate (:tokens response))]
                            (assoc token
                                   :id id
                                   :selected? (r/atom false)
                                   :highlighted? (r/atom false)))
                          (into [])))
      ;; Commit to `state`
      (#(reset! state %)))
  ;; Associate `depth`
  (let [tokens (:tokens response)]
    (doseq [[_token id] (enumerate tokens)]
      (depth id))))

(defonce loading? (r/atom false))
(defn submit
  "Submit the text to the API for syntax analysis"
  [input-text]
  (reset! loading? true)
  (let [body (-> {"document" {"type" "PLAIN_TEXT",
                              "language" "en"
                              "content" input-text}
                  "encodingType" "UTF8"}
                 clj->js
                 js/JSON.stringify)]
    (-> (js/fetch backend-api
                  (clj->js {:method "POST"
                            :headers {:Content-Type "application/json"}
                            :body body}))
        (.then (fn [res] (.json res)))
        (.then #(js->clj % :keywordize-keys true))
        (.then (fn [data]
                 (recalculate input-text data)
                 (reset! loading? false))))))

(defonce text-buffer (r/atom ""))
(defn input []
  [:textarea#input
   {:style {:width "100%", :font-size "1.5em"
            :color (if @dark-theme? "white" "black")
            :background (if @dark-theme? "black" "white")}
    :key "input-textarea"
    :rows "10"
    :placeholder "Type your text here!"
    :onChange (fn [e] (reset! text-buffer (-> e .-target .-value)))
    :value @text-buffer}])

(defn submit-btn []
  (cond
    ;; Loading animation
    @loading? [(r/adapt-react-class loading)
               {:style {:align-self "center", :margin "10px"}}]
    ;; The real button is here
    :else [:button#submit-btn {:style {:font-size "2em"
                                       :background "darkcyan"
                                       :align-self "center"}
                               :onClick (fn [e] (submit @text-buffer))}
           "Analyze!"]))

;; Component representing a token
(defn token-cpn [token]
  (let [tokens (:tokens @state)
        head-id (head-token-id token)
        head (tokens head-id)
        selected? (:selected? token)
        highlighted? (:highlighted? token)]
    [:span {:style {:padding "2px", :position "relative"
                    :font-size "1.5em"
                    :top (str (* 1 (:depth token)) "px")
                    :background (cond @selected? "blue"
                                      @highlighted? "darkcyan"
                                      :else "inherit")
                    :border-style (cond
                                    ;; Root verbs
                                    (= (:id token) head-id) "double"
                                    :else "none")}
            :onMouseEnter (fn [e]
                            (reset! selected? true)
                            (reset! (:highlighted? head) true))
            :onMouseLeave (fn [e]
                            (reset! selected? false)
                            (reset! (:highlighted? head) false))}
     (content token)]))

(defn split-tokens-newline
  "Returns a sequence of paragraphs/sequence of token (respecting order)"
  [tokens newlines]
  (cond (empty? tokens) []
        (empty? newlines) [tokens]
        :else (let [[left right]
                    (split-with #(< (begin-offset %) (first newlines))
                                tokens)]
                (concat [left]
                        (split-tokens-newline right (rest newlines))))))

;; Component to render the outcome of syntax analysis
(defn diagram []
  [:div#diagram {:style {:display "flex",
                         :flex-direction "column",
                         :border "dotted", :padding "0.5em"
                         :color (if @dark-theme? "white" "black")
                         :background (if @dark-theme? "black" "white")}}
   (for [group (split-tokens-newline (:tokens @state)
                                     (:newlines @state))
         :when (not-empty group)]
     [:div {:style {:display "flex", :flex-wrap "wrap"}}
      (for [token group] [token-cpn token])])])

(defn theme-selector []
  [:button {:style {:font-size "2em"
                    :max-width "300px"
                    :align-self "flex-end"}
            :onClick (fn [e] (swap! dark-theme? not))}
   (if @dark-theme? "Light" "Dark")])

;; Render the whole app
(defn container []
  [:div#container {:style {:display "flex"
                           :flex-direction "column"
                           :background (if @dark-theme? "black" "white")}}
   [theme-selector]
   [input]
   [submit-btn]
   [diagram]])

(rdom/render
 [container]
 (.getElementById js/document "app"))

(defn reload! [] (println "Code reloaded!"))

(defn main! []
  (println "`main.cljs` started!")
  (reset! text-buffer
          "About:
   This is a prototype web application to make long and complicated text easier to read. We can achieve this by making the syntactic structure of the text apparent as follows:
   1. We start by breaking the text down into sentences. Sentences are viewed to be independent of each other.
   2. We further break each sentence down into tokens. Loosely speaking, each token corresponds to a word.
   3. According to the theory in use, each token depends (supports) another token. For example: The word \"very\" depends on the word \"nice\" in the sentence \"This is very nice!\".
   We highlight this dependency like so: when you hover over a token, the token that it depends on will be highlighted.
   4. Some tokens aren't dependent on any other tokens. We call these \"roots\".
   Roots are outlined. Such as the first token in the sentence \"Ask not what your country can do for your.\"
   5. You may also notice that some tokens are lower than others. This is done to reflect their \"depths\". The rule is simple: if token \"x\" depends on token \"y\", \"x\" will be rendered lower than \"y\".

   That's it! For those interested on the theory, read up on \"dependency grammar\". The app is powered by Google Cloud Natural Language API.")
  (submit @text-buffer))

;; Todo:
;; Make a deployment build
;; Why does it break on the “” characters?
