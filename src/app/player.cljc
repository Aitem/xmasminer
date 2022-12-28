(ns app.player
  (:require [re-frame.core :as rf]))


(rf/reg-event-fx
 ::move-d
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :x] inc)]
     {:db new-db
      :app.ws/send {:event "move-x"
                    :data  (get-in new-db [:player :position :x])}})))


(rf/reg-event-fx
 ::move-a
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :x] dec)]
     {:db new-db
      :app.ws/send {:event "move-x"
                    :data  (get-in new-db [:player :position :x])}})))

(rf/reg-event-fx
 ::move-w
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :y] dec)]
     {:db new-db
      :app.ws/send {:event "move-y"
                    :data  (get-in new-db [:player :position :y])}})))

(rf/reg-event-fx
 ::move-s
 (fn [{db :db} _]
   (let [new-db (update-in db [:player :position :y] inc)]
     {:db new-db
      :app.ws/send {:event "move-y"
                    :data  (get-in new-db [:player :position :y])}})))

(rf/reg-event-db
 ::clear
 (fn [db _]
   #?(:cljs (.remove (js/document.getElementById "selected-menu-item")))
   (dissoc db :buildings-menu-item)
   ))
