(ns app.pages.game.end
  (:require [zframes.pages :as pages]
            [re-frame.core :as rf]
            [zframes.routing :as routing]
            [zframes.storage :as storage]
            [app.pages.index.model :as model]))

(def index-page ::index-page)


(rf/reg-event-fx
 ::restart
 (fn [{db :db} [_]]
   ;; restore hp
   ;; restore stats
   {:dispatch [:app.pages.index.model/prepare-patients {:data (:player db)}]}))

(defn score-calculator
  [keyword coll]
  (reduce (fn [acc p]
            (let [param (keyword p)]
              (+ acc param)))
          0
          coll))

(defn calc-stats [patients]
  (prn "->>>>>" patients)
  (let [{:keys [dead alive]} (group-by (fn [p]
                                         (if (get-in p [:deceased :boolean])
                                           :dead
                                           :alive)) patients)
        patients-alive           (count alive)
        patients-died            (count dead)
        money-left               (score-calculator :balance alive)
        patient-health-left      (score-calculator :health alive)
        dead-patients-money-left (score-calculator :balance dead)
        total-score              (- (* (+ money-left patient-health-left) patients-alive)
                                    (* dead-patients-money-left patients-died))]
    {:patients-alive           patients-alive
     :patients-died            patients-died
     :money-left               money-left
     :patient-health-left      patient-health-left
     :total-score              total-score
     :dead-patients-money-left dead-patients-money-left}))


(rf/reg-event-fx
  index-page
  [(rf/inject-cofx ::storage/get [:player])]
  (fn [{storage :storage  db :db} [pid phase params]]
    {:db (merge db storage)
     :json/fetch {:uri "/Patient"
                  :params {:general-practitioner (get-in storage [:player :id])}
                  :success {:event ::save-patients}}}))

(defn patients-map [resp]
  (->> resp :data :entry (map :resource)
       (reduce (fn [acc pt] (assoc acc (:id pt) pt)) {})
       (into (sorted-map))))

(rf/reg-event-fx
 ::save-patients
 (fn [{db :db} [_ resp]]
   (let [pts (patients-map resp)
         stats (calc-stats (vals pts))
         practitioner (assoc (:player db) :stats stats)]
     {:db (-> db
              (assoc :patients pts)
              (assoc ::stats stats))
      :json/fetch {:uri     (str "/Practitioner/" (:id practitioner))
                   :method  :put
                   :body    practitioner
                   :success {:event ::get-scoreboard}}})))

(rf/reg-event-fx
  ::get-scoreboard
  (fn [{:keys [db]} [_ & args]]
    {:json/fetch {:uri "/$sql"
                  :method :post
                  :body "\"select id as id
                                , resource #>> '{name,0,given,0}' as name
                                , resource #>> '{stats,total-score}' as score
                           from practitioner
                           where resource #>> '{stats,total-score}' is not null
                           order by (resource #>> '{stats,total-score}')::decimal desc
                           limit 10\""
                  :success {:event ::scoreboard}}}))


(rf/reg-event-fx
  ::scoreboard
  (fn [{:keys [db]} [_ resp]]
    {:db (assoc db ::scoreboard (:data resp))}))


(rf/reg-sub
  index-page
  (fn [db] db))


(rf/reg-sub
  :stats
  (fn [db _]
    (::stats db)))

(rf/reg-sub
  ::scoreboard
  (fn [db _]
    (::scoreboard db)))


(pages/reg-subs-page
 index-page
 (fn [{d :d :as  page} _]
   (let [stats   @(rf/subscribe [:stats])]
     [:div.inner.rpgui-container.framed.relative
      {:style {:height "calc(100vh - 35px)"}}
      [:h1 {:style {:font-size "250%"}} "Игра окончена!"]
      [:hr.golden]
      [:div {:style {:display :flex :justify-content :center}}
       [:div {:style {:text-align "center"}}
        [:p [:a {:href "https://healthmesamurai.edge.aidbox.app/ui/console#/notebooks/13d4bcc9-dd01-49d5-8b61-8ae32852dd36"
                 :target "blank"} "Used FHIR resources on aidbox.app"]]]]
      [:hr]
      [:table {:style {:width "100%"
                             :table-layout :fixed
                             :border "none"}}
       [:tbody
        [:tr
         [:td.score-td.score-right-td [:p "Выжило/умерло пациентов: "]]
         [:td.score-td.score-left-td [:p (str (:patients-alive stats) "/" (:patients-died stats))]]
         [:td.score-td.score-right-td [:p "Денег осталось: "]]
         [:td.score-td.score-left-td [:p (:money-left     stats)]]]
        [:tr
         [:td.score-td.score-right-td [:p "Оставшееся здоровье: "]]
         [:td.score-td.score-left-td [:p (:patient-health-left stats)]]
         [:td.score-td.score-right-td [:p "Деньги умерших: "]]
         [:td.score-td.score-left-td [:p (:dead-patients-money-left stats)]]]
        ]]
      [:hr]
      [:div {:style {:display :flex :justify-content :center}}
       [:div {:style {:text-align "center"}}
        [:h1 {:style {:font-size "210%"}} "Финальный счет: " (:total-score stats)]
        [:p (str "(" (:money-left stats) " + " (:patient-health-left stats) ") * " (:patients-alive stats)
                 " - " (:dead-patients-money-left stats) " * "(:patients-died stats)
                 " = " (:total-score stats))]]]
      [:hr]
      (let [scoreboard @(rf/subscribe [::scoreboard])]
        [:div
         [:div.rpgui-container.framed-golden.pos-initial
          {:style {:overflow-y "scroll"
                   :height "350px"}}
          [:table.score {:style {:border "none"}}
           [:thead {:style {:font-size "16px"}} [:tr [:th [:h1 "#"]] [:th [:h1 "Имя"]] [:th [:h1 "Счёт"]]]]
           [:tbody
            (for [[number {:keys [id name score]}] (map-indexed vector scoreboard)]
              ^{:key id}
              [:<>
               [:tr {:style {:margin "0px"}}
                [:td.no-border-td [:p (inc number)]]
                [:td.no-border-td [:p name]]
                [:td.no-border-td [:p score]]]
               (if (= id (get-in page [:player :id]))
                 [:tr {:style {:margin "0px"}}
                  [:td.no-border-td {:style {:margin-bottom "0", :margin-top "0"}}[:hr.golden]]
                  [:td.no-border-td {:style {:margin-bottom "0", :margin-top "0"}}[:hr.golden]]
                  [:td.no-border-td {:style {:margin-bottom "0", :margin-top "0"}}[:hr.golden]]]
                 [:tr {:style {:margin "0px"}}
                  [:td.no-border-td {:style {:margin-bottom "0", :margin-top "0"}}[:hr]]
                  [:td.no-border-td {:style {:margin-bottom "0", :margin-top "0"}}[:hr]]
                  [:td.no-border-td {:style {:margin-bottom "0", :margin-top "0"}}[:hr]]])
               ])]]]])

      [:div.rpgui-center
       [:div {:style {:width "300px" :margin "0 auto"}}]]
      [:hr]
      [:div.rpgui-center
       [:button.rpgui-button.rpgui-cursor-default
        {:on-click #(rf/dispatch [::restart])}
        [:p "Сыграть снова"]]]
      [:br]])))
