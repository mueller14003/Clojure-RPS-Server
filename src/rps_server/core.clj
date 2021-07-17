(ns rps-server.core
  (:require [clojure.java.io :as io])
  (:require [clojure.repl :as repl])
  (:import (java.net ServerSocket)))

(repl/set-break-handler! (fn [_] (println "\nClosing Server")))

(def rps-map {:r {:r "d" :p "l" :s "w" :q "q"}
              :p {:r "w" :p "d" :s "l" :q "q"}
              :s {:r "l" :p "w" :s "d" :q "q"}
              :q {:r "q" :p "q" :s "q" :q "q"}})

(defn receive-rps [socket]
  (let [input (.read (io/reader socket))]
    (if (< input 0)
      "q"
      (str (char input)))))

(defn rps-result [p1 p2]
  (get (get rps-map (keyword p1) {:q "q"}) (keyword p2) "q"))

(defn send-message [socket message]
  (let [writer (io/writer socket)]
    (.write writer message)
    (.flush writer)))

(defn close-clients [p1-socket p2-socket]
  (if (not (.isClosed p1-socket))
    (send-message p1-socket "q")
    (println "p1-socket already closed"))
  (if (not (.isClosed p2-socket))
    (send-message p2-socket "q")
    (println "p2-socket already closed")))

(defn -main
  [& args]
  (if (not (= (count args) 1))
    (do (println "ERROR: INVALID NUMBER OF INPUTS")
        (println "Please run again with the port number as a command-line argument.")
        (println "Example:\n> lein run 6789")
        (System/exit 0))

    (with-open [server-socket (ServerSocket. (Integer/parseInt (first args)))]
      (println "The Server is ready to receive")
      (with-open [p1-socket (.accept server-socket)
                  p2-socket (.accept server-socket)]

        (println (format "\nPlayer 1 - %s" (.getRemoteSocketAddress p1-socket)))
        (println (format "Player 2 - %s" (.getRemoteSocketAddress p2-socket)))

        (send-message p1-socket "1")
        (send-message p2-socket "1")

        (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (close-clients p1-socket p2-socket))))

        (loop [p1-input (receive-rps p1-socket)
               p2-input (receive-rps p2-socket)]

          (println (format "\nPlayer 1 input: %s" p1-input))
          (println (format "Player 2 input: %s" p2-input))

          (let [p1-result (rps-result p1-input p2-input)
                p2-result (rps-result p2-input p1-input)]

            (println (format "\nPlayer 1 result: %s" p1-result))
            (println (format "Player 2 result: %s" p2-result))
            
            (if (.isClosed p1-socket)
              (if (.isClosed p2-socket)
                (System/exit 0)
                (send-message p2-socket "q"))
              (send-message p1-socket p1-result))

            (if (.isClosed p2-socket)
              (if (.isClosed p1-socket)
                (System/exit 0)
                (send-message p1-socket "q"))
              (send-message p2-socket p2-result))

          (when (not (or (= p1-input "q") (= p2-input "q") (.isClosed p1-socket) (.isClosed p2-socket)))
            (recur (receive-rps p1-socket)
                   (receive-rps p2-socket)))))

        (println "\nClosing Server")))))