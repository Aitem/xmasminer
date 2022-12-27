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

(defn view [{{pos :position} :player gmap :map res :res belt :belt :as page}]
  [:div#screen
   [menu]
   [:span#info (str pos)]
   [:div#map
    (for [p (:players page)]
      [:div#player {:key (hash p)
                    :style {:background (:color p)
                            :grid-column (-> p :position :x)
                            :grid-row    (-> p :position :y)}}
       [:div {:style {:color "black"
                      :position "absolute"
                      :text-align "center"
                      :margin-top "-20px"}}
        (:name p)]])

    [:<>
     (map
      (fn [[[x y] [type opts]]]
        [:div.belt {:key (hash (str x y type))
                    :class (str "belt " (get belt-dir opts))
                    :style {:grid-column x :grid-row    y}}])
      gmap)]



    ]
   ]

  )
