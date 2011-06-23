(ns clojure.core)

; The print functions write to *out*, so let's redefine it
; so that we can show printed objects within the REPL
(def outstrm (java.io.ByteArrayOutputStream.))
(def *out* (java.io.OutputStreamWriter. outstrm))

(def about (str "Oasis Digital (http://oasisdigital.com) is a consulting firm located in St. Louis, Missouri.\n"
                "The GitHub repository for this project can be found at: https://github.com/kylecordes/clj-repl-applet\n"))

(ns com.oasisdigital.clj-repl-applet.core
  (import javax.swing.JApplet)
  (:gen-class
    :extends javax.swing.JApplet))

(import [javax.swing JFrame JEditorPane JPanel JScrollPane])
(import [javax.swing.text Document DocumentFilter SimpleAttributeSet StyledDocument html.HTMLDocument html.StyleSheet])
(import [java.awt GridBagLayout Dimension GridBagConstraints Insets Font Color])
(require 'clojure.string)
(def display-var) ; Required to be a variable for the print methods

(def text "Clojure REPL\n------------\nThis program was created by Oasis Digital. Type 'about' for more information.")
(def editable-start 5)

(defn append-display [display txt]
  "Appends the string txt to the JEditorPane display."
  (let [doc (.getDocument display)]
    (.insertString doc (.getLength doc) txt nil)
    (def editable-start (.getLength doc))))

(defn format-exception [e]
  "Returns the exception e as a colored string."
    (str (first (clojure.string/split (str e) #" .NO_SOURCE_FILE"))))

(defn exec-cmd [command]
  (let [result (try (load-string command)
               (catch Exception e (format-exception e)))]
    ; Force evaluation of lazy sequences
    (if (= (class result) clojure.lang.LazySeq)
      (str (seq (doall result)))
      (if (= result nil)
        "nil"
        (str result)))))

(defn remove-trailing-newline [string]
  (let [last-char (last string)]
    (if (= last-char "\n")
      (rest (reverse string))
      string)))

(defn run-cmd [display string]
  "Executes the command currently being typed and outputs the result to the display."
  (let [doc (.getDocument display) result (exec-cmd string)]
    ; The command line Clojure REPL outputs something like (print 5) immediately even though print
    ; doesn't call flush like println does. So let's emulate that feature.
    (flush)
    (let [print-buffer (.toString clojure.core/outstrm)]
      (if-not (empty? print-buffer)
        (do
          (append-display display (str "\n" (remove-trailing-newline print-buffer)))
          (.reset clojure.core/outstrm))))
    (append-display
      display
      (str "\n" result))
    (append-display display "\n=> ")))

(defn get-cmd [display]
  (let [doc (.getDocument display)]
    (str (.getText doc editable-start (- (.getLength doc) editable-start)))))

(defn configure-gui [{:keys [frame panel display scrollpane c applet?] :as repl}]
  (.setLayout panel (GridBagLayout.))
  (.setPreferredSize panel (Dimension. 600 400))
  ; Settings for JEditorPane (where commands are displayed)
  (.setEditable display true)
  (.addRule (.getStyleSheet (.getDocument display)) "body { font-family: Consolas, Courier New; font-size: 11px; }")
  (.setMargin display (Insets. 10 10 10 10))
  (.setBackground display Color/white)
  (append-display display text)
  (append-display display "\n=> ")
  (set! (. c fill) (GridBagConstraints/BOTH))
  (set! (. c gridy) 0)
  (.setDocumentFilter (.getDocument display) (proxy [DocumentFilter] []
                                               ; Only allows the "remove" call to pass through to the document
                                               ; if the cursor is in a legal position
                                               (remove [fb offset length]
                                                       (if (>= offset editable-start)
                                                         (proxy-super remove fb offset length)))
                                               ; Interestingly, replace is called when text is inserted
                                               ; even though the documentation says that insertString is reserved
                                               ; for this purpose.
                                               (replace [fb offset length text attrs]
                                                        (if (>= offset editable-start)
                                                            (if (= text "\n")
                                                              (run-cmd display (get-cmd display))
                                                              (proxy-super replace fb offset length text attrs))))))
  (set! (. c weightx) 1)
  (set! (. c weighty) 1)
  (set! (. c gridheight) 1)
  (.add panel scrollpane c)
  (. frame add panel)
  (if-not applet?
    (doto frame
      (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
      .pack
      (.setVisible true)))

  ; When the GUI is initialized, a bunch of information is printed to *out* about the JFrame object.
  ; We don't want that showing up in the REPL, so reset the buffer.
  (.reset clojure.core/outstrm))

(defn start [applet? frame]
  (let [display (JEditorPane. "text/html" "")]
    (configure-gui {:frame frame
                    :panel (JPanel.)
                    :display display
                    :scrollpane (JScrollPane. display)
                    :c (GridBagConstraints.)
                    :applet? applet?})))

(defn -main [& args]
  (start false (JFrame.)))

(defn -init [applet]
  (start true applet))
