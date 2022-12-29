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
         y (+ vp-y (dec vp-by))]
     (when (allow-building-create? db (:buildings-menu-item db))
       {:app.ws/send {:event "create-building"
                      :data {:id (get-in db [:buildings-menu-item :id])
                             :x x
                             :y y
                             :dir (get-in db [:buildings-menu-item :dir])
                             :inputs (get-in db [:buildings-menu-item :inputs])
                             :output (get-in db [:buildings-menu-item :output])
                             :ticks (get-in db [:buildings-menu-item :ticks])}}}))))

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
         y (+ vp-y (dec vp-by))]
     {:app.ws/send {:event "remove-building" :data {:x x :y y}}})))

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
     {:id :fc :dir :r :inputs {:w 2 :l 2} :output :b :ticks 2}]}))

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
  (case level
    1 10
    2 20
    3 40
    4 80
    5 160
    nil 40))

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
