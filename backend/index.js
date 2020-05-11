/* This server does one thing: relay POST requests to Google's syntax api */

// Requires & setup variables
require("dotenv").config();
let express = require("express");
let fetch = require("node-fetch");
let rateLimit = require("express-rate-limit");
var cors = require("cors");
let app = express();
let port = 3000;
let key = process.env.key;
let api = `https://language.googleapis.com/v1/documents:analyzeSyntax?key=${key}`;

// We're gonna be behind a proxy
app.set('trust proxy', 1);

// Limit each IP to one request/sec
let limiter = rateLimit({ windowMs: 1000, max: 2 })
app.use(limiter);

// Allow CORS from any origin
app.use(cors());

// Body parser
app.use(express.json());

// Test route
app.get("/", (req, res) => {res.send("This is just a test route, use POST instead")})
// Relay whatever post request to the API
app.post("/", (req, res) => {
    fetch(api, { method: "POST", body: JSON.stringify(req.body) })
        .then(apiRes => apiRes.json())
        .then(body => res.json(body))
});

app.listen(port, () => console.log(`Relay server listening on port ${port}`));
