(ns app.player
  (:require [re-frame.core :as rf]))


(rf/reg-event-db
 ::move-d
 (fn [db _]
   (prn "here D")
   (update-in db [:player :position :x] inc)))

(rf/reg-event-db
 ::move-a
 (fn [db _] (update-in db [:player :position :x] dec)))

(rf/reg-event-db
 ::move-w
 (fn [db _] (update-in db [:player :position :y] dec)))

(rf/reg-event-db
 ::move-s
 (fn [db _] (update-in db [:player :position :y] inc)))
