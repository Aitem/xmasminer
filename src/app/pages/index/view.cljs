(ns app.pages.index.view
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [app.pages.index.model :as model]))



(pages/reg-subs-page
 model/index-page
 (fn [{d :d :as  page} _]
   (let [gettext (fn [e] (-> e .-target .-value))
         emit    (fn [e] (rf/dispatch [::model/practitioner-name (gettext e)]))
         pr-name @(rf/subscribe [::model/practitioner-name])]
     [:div.inner.rpgui-container.framed.relative
      {:style {:text-align "center"}}
      [:h1 {:style {:font-size "250%"}} "Health me, Samurai!"]
      [:hr.golden]
      [:p "На дворе 2040 год, коронавирус побежден, но появился новый, более опасный вирус, противостояние с которым только началось."]
      [:p "При заражении требуется неустанное внимание врача и госпитализация, так как каждый день вирус мутирует в теле, и появляются новые симптомы."]
      [:p "Чтобы выжить, надо продержаться 10 дней, после этого зараза отступает и появляется полный иммунитет."]
      [:p "В этой игре Вы - врач в клинике Святого Николая, у Вас три пациента и аптечка лекарств. Выбирайте лекарства и лечите больных пока у них не кончатся деньги или не закончатся очки хода."]
      [:hr]

      [:span
       [:span [:img.pt-icn {:src "./img/thermometer.png"}] "Температура"]
       [:span " | "]
       [:span [:img.pt-icn {:src "./img/tonometer.png"}] "Давление"]
       [:span " | "]
       [:span [:img.pt-icn {:src "./img/sugar.png"}] "Сахар"]
       [:span " | "]
       [:span [:img.pt-icn {:src "./img/bacteria.png"}] "Бактерии"]
       [:span " | "]
       [:span [:img.pt-icn {:src "./img/diarrhea.png"}] "ЖКТ"]]

      [:hr]
      [:br]


      [:div.rpgui-center
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
