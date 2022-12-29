(ns server.buildings)

(defn belt [direction]
  [:belt {:direction direction}])

(defn belt-left []
  (belt :left))

(defn belt-right []
  (belt :right))

(defn belt-up []
  (belt :up))

(defn belt-down []
  (belt :down))
