(ns syntax-diagram.main
  (:require
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [cljs.reader :refer [read-string]]
   [clojure.string :as str]
   [cljs.pprint :refer [cl-format]]))

(defn reload! []
  (println "Code updated!"))

(defn main! []
  (println "`main.cljs` started!"))

(defonce response (r/atom ""))

(defn submit [input-text]
  (let [;; This is to our backend, which will then be relayed to Google API
        req-url "http://localhost:3000/"
        xhr (new js/XMLHttpRequest)]
    (.open xhr "POST", req-url, false)
    (.setRequestHeader xhr "Content-type", "application/json")
    (let [body (-> {"document" {"type" "PLAIN_TEXT"
                                "language" "en"
                                "content" input-text}
                    "encodingType" "UTF8"}
                   clj->js
                   js/JSON.stringify)]
      (.send xhr body))
    (reset! response (.-responseText xhr))))

(defn input []
  (let [keymap (atom {})  ;; Remember the keys that are pressed
        text-buffer (r/atom "")]
    [:textarea#input
     {:style {:width "100%"}
      :rows "20"
      :placeholder "Type your text here (Press 'Shift+Enter' to submit)!"
      :onChange (fn [e] (reset! text-buffer (-> e .-target .-value)))
      :onKeyUp (fn [kbe] (swap! keymap dissoc (.-key kbe)))
      :onKeyDown (fn [kbe]
                   (swap! keymap assoc (.-key kbe) true)
                   (when  ;; If `Shift+Enter` is pressed, then submit state
                       (and (@keymap "Shift") (@keymap "Enter"))
                     (submit @text-buffer)))}]))

(defn diagram []
  [:div#diagram @response])

(rdom/render  ;; Render the whole app
 [:<>
  [:span "Press " [:b "Shift+Enter"] " to submit"]
  [input]
  [diagram]]
 (.getElementById js/document "app"))
