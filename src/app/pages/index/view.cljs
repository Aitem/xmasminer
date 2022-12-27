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
(defn menu
  []
  [:div {:style {:padding-top "20px"}}
   [:label "Name "]
   [:input {:type "text" :on-change #(rf/dispatch [::model/change-name (.. % -target -value)])}]
   ])

(pages/reg-subs-page
 model/index-page
 (fn [{{pos :position} :player res :res belt :belt :as page} _]
   [:div#screen
    [menu]
    [:span#info (str pos)]
    [:div#map
     (for [p (:players page)]
       [:div#player {:style {:background (:color p)
                             :grid-column (-> p :position :x)
                             :grid-row    (-> p :position :y)}}
        [:div {:style {:color "black"
                       :position "absolute"
                       :text-align "center"
                       :margin-top "-20px"}}
         (:name p)]])

     [:<>
      (map
       (fn [[id [x1 x2 y1 y2 d]]]
         [:div.belt {:id id
                     :key id
                     :class (str "belt " (get belt-dir d))
                     :style {:grid-column (str x1 " / " x2)
                             :grid-row    (str y1 " / " y2)}}])

       belt)]

     [:<>
      (map
       (fn [[id [x y c]]]
         [:div.res.char {:key id
                         :class "char-h"
                         :style {:grid-column (str x " / " (+ 2 x))
                                 :grid-row    y}}])

       res)]


     ]
    ]

   ))
