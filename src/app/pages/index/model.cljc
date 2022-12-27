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
   (select-keys db [:player :players :buildings :res])))

(rf/reg-event-fx
 ::change-name
 (fn [_ [_ value]]
   {:app.ws/send {:event "change-name" :data value}}))
