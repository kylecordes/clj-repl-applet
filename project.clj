(defproject clj-repl-applet "1.0.0-SNAPSHOT"
  :description "A Clojure REPL packaged to be run as a standalone Swing application and an applet."
  :url "https://github.com/kylecordes/clj-repl-applet"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :aot [com.oasisdigital.clj-repl-applet.applet]
  :main com.oasisdigital.clj-repl-applet.applet
  :warn-on-reflections true
  :uberjar-name "clj-repl-applet.jar"
  :dependencies [[org.clojure/clojure "1.2.1"]])

