(ns app.pages.index.model
  (:require [re-frame.core :as rf]
            [zframes.storage :as storage]
            [zframes.routing :as routing]
            [zframes.pages :as pages]))

(def index-page ::index-page)

(def genders ["male" "female"])

(def pt-names
  {"male"   #{"Arnbo" "Ilugaars" "Kuumtoq" "Dulao"
              "Uruazoc" "Jiusave" "Somerham" "Lonwick"
              "Qassiars" "Buenapant" "Yarcester" "Preshurst" "Buxbron"}
   "female" #{"Kullorsa" "Lecona" "Arbornora" "Bricona"
              "Ilulisna" "Siorapa" "Coamora" "Guadora"
              "Dorrial" "Onostone" "Harpstone"}})

(def pt-avatars
  {"male"   #{"char_1_male.png" "char_2_male.png" "char_4_male.png" "char_6_male.png"
             "char_7_male.png" "char_9_male.png" "char_14_male.png"}
   "female" #{"char_3_female.png" "char_5_female.png" "char_8_female.png" "char_10_female.png"
              "char_11_female.png" "char_12_female.png" "char_13_female.png"}})

(rf/reg-sub
 ::practitioner-name
 (fn [db _]
   (:practitioner-name db)))

(rf/reg-event-db
 ::practitioner-name
 (fn [db [_ name]]
   (-> db
       (assoc :practitioner-name name))))




(rf/reg-event-fx
 ::run-game
 (fn [{db :db} [evid _]]
   {::routing/redirect {:ev :app.pages.game.model/index-page}}))

(rf/reg-sub
 index-page
 (fn [db _]
   (select-keys db [:player :players :buildings :res :mines
                    :cursor :buildings-menu-item])))

(rf/reg-event-fx
 ::change-name
 (fn [_ [_ value]]
   {:app.ws/send {:event "change-name" :data value}}))

(rf/reg-event-db
 ::select-buildings-menu-item
 (fn [db [_ item]]
   (assoc db :buildings-menu-item item)))

(rf/reg-event-fx
 ::create-seleted-building
 (fn [{db :db} [_ x y]]
   {:app.ws/send {:event "create-building"
                  :data {:x x :y y
                         :id (get-in db [:buildings-menu-item :id])
                         :dir (get-in db [:buildings-menu-item :dir])}}}))


(rf/reg-event-fx
 ::remove-building
 (fn [db [_ x y]]
   {:app.ws/send {:event "remove-building" :data {:x x :y y}}}))

(rf/reg-sub
 ::selected-menu-item
 (fn [db _]
   (:buildings-menu-item db)))

(rf/reg-sub
 ::buildings-menu
 :<- [::selected-menu-item]
 (fn [selected-menu-item _]
   {:items [{:id :b :dir :u :class ["belt-u" "belt" (when (= :b (:id selected-menu-item)) "selected-item")]}]}))

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
   (assoc db :cursor {:x x :y y})))
