// Just a simple server for static files
let express = require("express");
let app = express();
app.use(express.static("resources/public"))

let port = process.env.PORT || 3000
app.listen(port, () => console.log(`Relay server listening on port ${port}`));
