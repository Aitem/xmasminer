(ns app.pages.index.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.routing :as routing]
            [zframes.pages :as pages]))

(def index-page ::index-page)

(rf/reg-sub
 index-page
 (fn [db _]
   (select-keys db [:player :players :buildings :res :mines
                    :world
                    :fabrics
                    :cursor :buildings-menu-item :viewport :zoom-level])))

(rf/reg-event-fx
 ::change-name
 (fn [_ [_ value]]
   {:app.ws/send {:event "change-name" :data value}}))

(rf/reg-event-db
 ::select-buildings-menu-item
 (fn [db [_ item]]
   (assoc db :buildings-menu-item item)))

(defn allow-miner-create?
  [cursor viewport mines]
  (get mines [(+ (:x viewport) (dec (:x cursor)))
              (+ (:y viewport) (dec (:y cursor)))]))

(defn allow-building-create?
  [page item]
  (cond
    (= :m (:id item))
    (allow-miner-create? (:cursor page) (:viewport page) (:mines page))
    :else true))

(defn make-fabric
  [{[x y] :position direction :direction output :output inputs :inputs :as options}]
  (into {}
        (map-indexed
         (fn [index [tp cnt]]
           [(case direction
              :r [(+ x index) y]
              :l [(- x index) y]
              :d [x (+ y index)]
              :u [x (- y index)])
            (cond-> [:f :r (merge
                            {:input tp :amount cnt :main [x y] }
                            (when (= 0 index)
                              {:dir direction  :inputs inputs :ticks (:ticks options) :output (:output options)}))])])
         inputs)))

(rf/reg-event-fx
 ::create-seleted-building
 (fn [{db :db} _]
   (let [pid (get-in db [:player :id])
         player (first (filter #(= pid (:id %)) (:players db)))
         vp-h (get-in db [:viewport :h])
         vp-w (get-in db [:viewport :w])
         p-x (get-in player [:position :x])
         p-y (get-in player [:position :y])
         vp-x (- p-x (int (/ vp-w 2)))
         vp-y (- p-y (int (/ vp-h 2)))
         vp-bx (get-in db [:cursor :x])
         vp-by (get-in db [:cursor :y])
         x (+ vp-x (dec vp-bx))
         y (+ vp-y (dec vp-by))
         fabrics (make-fabric
                  {:position  [x y]
                   :direction (get-in db [:buildings-menu-item :dir])
                   :inputs    (get-in db [:buildings-menu-item :inputs])
                   :output    (get-in db [:buildings-menu-item :output])
                   :ticks     (get-in db [:buildings-menu-item :ticks])})]
     (when (:buildings-menu-item db)
       (when (and (allow-building-create? db (:buildings-menu-item db))
                  (and (not= :h (first (get (:buildings db) [x y])))
                       (not (some #(= :h (first (get (:buildings db) %)))
                                  (keys fabrics)))))
         (merge 
          (when (= :fc (get-in db [:buildings-menu-item :id]))
            {:dispatch [:app.player/clear]})
          {:db (if (= :fc (get-in db [:buildings-menu-item :id]))
                 (update db :fabrics merge fabrics)
                 (assoc-in db [:buildings [x y]] [(get-in db [:buildings-menu-item :id])
                                                  (get-in db [:buildings-menu-item :dir])
                                                  (when (= :m (get-in db [:buildings-menu-item :id]))
                                                    (get-in db [:mines [x y]]))]))
           :app.ws/send {:event "create-building"
                         :data {:id (get-in db [:buildings-menu-item :id])
                                :x x
                                :y y
                                :mine  (get-in db [:mines [x y]])
                                :dir (get-in db [:buildings-menu-item :dir])
                                :inputs (get-in db [:buildings-menu-item :inputs])
                                :output (get-in db [:buildings-menu-item :output])
                                :ticks (get-in db [:buildings-menu-item :ticks])}}}))))))

(rf/reg-event-fx
 ::remove-building
 (fn [{db :db} _]
   (let [pid (get-in db [:player :id])
         player (first (filter #(= pid (:id %)) (:players db)))
         vp-h (get-in db [:viewport :h])
         vp-w (get-in db [:viewport :w])
         p-x (get-in player [:position :x])
         p-y (get-in player [:position :y])
         vp-x (- p-x (int (/ vp-w 2)))
         vp-y (- p-y (int (/ vp-h 2)))
         vp-bx (get-in db [:cursor :x])
         vp-by (get-in db [:cursor :y])
         x (+ vp-x (dec vp-bx))
         y (+ vp-y (dec vp-by))
         building (or (get (:buildings db) [x y])
                      (get (:fabrics db) [x y]))]
     (when (not= :h (first building))
       {:db (if (= :f (first building))
              (update db :fabrics
                      (fn [bs]
                        (into {}
                              (remove (fn [[_ [_ _ opts]]]
                                        (= (:main opts) (get-in building [2 :main])))
                                      bs))))
              (update db :buildings dissoc [x y]))
        :app.ws/send {:event "remove-building" :data {:x x :y y}}}))))

(rf/reg-sub
 ::selected-menu-item
 (fn [db _]
   (:buildings-menu-item db)))

(rf/reg-sub
 ::buildings-menu
 :<- [::selected-menu-item]
 (fn [selected-menu-item _]
   {:items
    [{:id :b  :dir :u :class ["t" "belt-u" "belt" (when (= :b (:id selected-menu-item)) "selected-item")]}
     {:id :m  :dir :r :class ["t" "miner" "miner-r" (when (= :m (:id selected-menu-item)) "selected-item")]}
     {:id :fc :dir :r :class ["t" "mine-a" (when (= :m (:id selected-menu-item)) "selected-item")] :inputs {:b 2} :output :a :ticks 2}
     ;; Circuites
     {:id :fc :dir :r :class ["t" "mine-c" (when (= :m (:id selected-menu-item)) "selected-item")] :inputs {:m 1 :wg 1} :output :c :ticks 2}
     {:id :fc :dir :r :class ["t" "mine-cr" (when (= :m (:id selected-menu-item)) "selected-item")] :inputs {:m 1 :wr 1} :output :cr :ticks 2}
     {:id :fc :dir :r :class ["t" "mine-cb" (when (= :m (:id selected-menu-item)) "selected-item")] :inputs {:m 1 :w 1} :output :cb :ticks 2}
     ;; ORBS
     {:id :fc :dir :r :class ["t" "mine-ob" (when (= :m (:id selected-menu-item)) "selected-item")] :inputs {:a 1 :l 1 :cb 1} :output :ob :ticks 2}
     {:id :fc :dir :r :class ["t" "mine-og" (when (= :m (:id selected-menu-item)) "selected-item")] :inputs {:a 1 :l 1 :c 1} :output :og :ticks 2}
     {:id :fc :dir :r :class ["t" "mine-or" (when (= :m (:id selected-menu-item)) "selected-item")] :inputs {:a 1 :l 1 :cr 1} :output :or :ticks 2}
     ]}))

(defn fabric-rotate
  [cursor dir inputs]
  (map-indexed
   (fn [index input]
     [(case dir
        :r [(+ (:x cursor) index) (:y cursor)]
        :l [(- (:x cursor) index) (:y cursor)]
        :d [(:x cursor) (+ (:y cursor) index)]
        :u [(:x cursor) (- (:y cursor) index)])
      (first input)])
   inputs))

(rf/reg-event-db
 ::seleted-building-rotate
 (fn [db [_]]
   (update-in db [:buildings-menu-item :dir]
           (fn [dir]
             (get {:u :r
                   :r :d
                   :d :l
                   :l :u} dir)))))

(defn zoom-level->tile-size [level]
  (if level
    (let [scale-factor (js/Math.pow 2 (- level 3))
          tile-size (int (* scale-factor 40))]
      tile-size)
    40))

(rf/reg-event-db
 ::map-cursor
 (fn [db [_ true-x true-y]]
  (let [zoom-level (:zoom-level db)
        tile-size (zoom-level->tile-size zoom-level)
        x (int (/ true-x tile-size))
        y (int (/ true-y tile-size))]
   (assoc db :cursor {:x (inc x)
                      :y (inc y)}))))

(comment
  
  (fabric-rotate {:x 0 :y 0} :r {:a 1 :b 2})
  (fabric-rotate {:x 0 :y 0} :b {:a 1 :b 2})
  )
