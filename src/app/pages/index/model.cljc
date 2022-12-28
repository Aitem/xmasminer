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
                    :cursor :buildings-menu-item :viewport])))

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
   (let [vp (:viewport db)
         vp-bx (get-in db [:cursor :x])
         vp-by (get-in db [:cursor :y])
         vp-x (:x vp)
         vp-y (:y vp)
         x (+ vp-x (dec vp-bx))
         y (+ vp-y (dec vp-by))]
     (when (allow-building-create? db (:buildings-menu-item db))
       {:app.ws/send {:event "create-building"
                      :data {:x x :y y
                             :id (get-in db [:buildings-menu-item :id])
                             :dir (get-in db [:buildings-menu-item :dir])}}}))))

(rf/reg-event-fx
 ::remove-building
 (fn [{db :db} _]
   (let [vp (:viewport db)
         vp-bx (get-in db [:cursor :x])
         vp-by (get-in db [:cursor :y])
         vp-x (:x vp)
         vp-y (:y vp)
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
    [{:id :b :dir :u :class ["t" "belt-u" "belt" (when (= :b (:id selected-menu-item)) "selected-item")]}
     {:id :m :dir :r :class ["t" "miner" "miner-r" (when (= :m (:id selected-menu-item)) "selected-item")]}]}))

(rf/reg-event-db
 ::seleted-building-rotate
 (fn [db [_]]
   (update-in db [:buildings-menu-item :dir]
           (fn [dir]
             (get {:u :r
                   :r :d
                   :d :l
                   :l :u} dir)))))

(rf/reg-event-db
 ::map-cursor
 (fn [db [_ x y]]
   (assoc db :cursor {:x (inc x)
                      :y (inc y)})))
