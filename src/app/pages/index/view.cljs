(ns app.pages.index.view
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [app.pages.index.model :as model]))


(def belt-dir {:u "belt-u"
               :d "belt-d"
               :r "belt-r"
               :l "belt-l"
               :ur "belt-ur"
               })

(pages/reg-subs-page
 model/index-page
 (fn [{{pos :position} :player belt :belt :as page} _]
   [:div#screen
    [:span#info (str pos)]
    [:div#map

      [:div#player {:style {:grid-column (:x pos)
                            :grid-row    (:y pos)}}]

     [:<>
      (map
       (fn [[id [x1 x2 y1 y2 d]]]
         [:div.belt {:id id
                     :class (str "belt " (get belt-dir d))
                     :style {:grid-column (str x1 " / " x2)
                             :grid-row    (str y1 " / " y2)}}])

       belt)]



     ]
    ]



   #_(let [gettext (fn [e] (-> e .-target .-value))
         emit    (fn [e] (rf/dispatch [::model/practitioner-name (gettext e)]))
         pr-name @(rf/subscribe [::model/practitioner-name])]
     [:div.inner.rpgui-container.framed.relative
      {:style {:text-align "center"}}
      [:h1 {:style {:font-size "250%"}} "XmasMiner"]
      [:p "Mine Your Xmas"]
      [:hr]


      [:hr]
      [:br]


      #_[:div.rpgui-center
       [:div {:style {:width "300px" :margin "0 auto"}}
        [:label "Как тебя зовут?"]
        [:input {:type "text"
                 :minLength 2
                 :placeholder "Введи имя"
                 :value pr-name
                 :on-change emit}]]]



      [:br]
      [:div.rpgui-center
       (if (<= 2 (count (str pr-name)))
         [:button.rpgui-button.rpgui-cursor-default
          {:on-click #(rf/dispatch [::model/start-game @(rf/subscribe [::model/practitioner-name])])}
          [:p "Начать"]]
         [:button.rpgui-button.rpgui-cursor-default.grayscale
          {:disabled true}
          [:p "Начать"]])]
      [:br]])))
