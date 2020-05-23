# Intro
This is a proptotype web application to make long text easier to read. We can achieve this highlighting their syntactic structure.

Proptotype website: https://syntax-diagram.herokuapp.com/

# How it works
1. We start by breaking the text down into sentences. Sentences are viewed to be independent of each other.
2. We further break each sentence down into tokens. Loosely speaking, each token corresponds to a word.
3. According to the theory in use, each token depends (supports) another token. For example: The word "very" depends on the word "nice" in the sentence "This is very nice!".
We highlight this dependency like so: when you hover over a token, the token that it depends on will be highlighted.
4. Some tokens aren't dependent on any other tokens. We call these "roots".
Roots are outlined. Such as the first token in the sentence "Ask not what your country can do for your."
5. You may also notice that some tokens are lower than others. This is done to reflect their "depths". The rule is simple: if token "x" depends on token "y", "x" will be rendered lower than "y".

That's it! For those interested on the theory, read up on "dependency grammar". The app is powered by Google Cloud Natural Language API.
