(ns server.buildings)

(defn belt [direction]
  [:belt {:w 1 :h 1} {:direction direction}])

(defn hub [direction resource limit cnt]
  [:hub {:w 1 :h 1} {:direction direction
                     :resource resource
                     :limit limit
                     :count cnt}])

(defn tree []
  [:tree {:w 1 :h 1} {:direction :up
                      :limit 10
                      :count 0}])
