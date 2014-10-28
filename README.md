# clojure-dojo-2014-11-27

This is the project skeleton for the the '(clojure vienna) dojo.
I's a clojurescript dev environment that can be used with cider and lighttable.

The project contains a small demo app, that demonstrates om, bootstrap-cljs and ajax calls.
The server part contains a dummy in-memory database and demonstrates rest routing with moustache
and liberator.

## Usage
Start the dev server with

    lein do clean, cljsbuild once, trampoline run

and in a second shell

    lein trampoline cljsbuild auto dev

Then set up the browser repl with

### Cider
- Connect your nrepl to `localhost:4040`
- Call `(dojo.dev/start-browser-repl)`
- Connect your browser to `localhost:8080`

### Lighttable
- Add the project folder to lighttable.
- Create an external nrepl connection to `localhost:4040`
- Create a browser connection and navigate to `localhost:8080`

## License

Copyright © 2014 Herwig Hochleitner

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
