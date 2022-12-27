((ns user
  (:require [cider.nrepl :as ci]
            [clojure.java.io :as io]
            ;; wellnecity.core
            [libox.v2 :as box]
            [nrepl.middleware :as mw]
            [nrepl.server :as nrepl-server]
            [refactor-nrepl.middleware :as refactor]
            [clojure.string :as str]))

(def app
  {:import {:fhir-4.0.0 {}}
   :version    15})


(defn start
  [app]
  (let [_ (box/start app)
       #_#_ zen-cfgs (zen.cfg/get-configs @manifest)]
    :start-ok))

(defonce dev-server (atom nil))


(defn dev-stop []
  (when @dev-server
    (box/stop @dev-server))
  (reset! dev-server nil))

(defn dev-restart []
  (dev-stop)
  (let [app' (-> app
                 (assoc-in [:config :web :port] 8084))]
    (reset! dev-server (start app')))
  :ok)

(defn restart []
  (prn "Restarting.")
  (dev-restart))

(def handler
  (let [mws
        (->> ci/cider-middleware
             (map resolve)
             (concat nrepl-server/default-middleware))
        stack
        (->> (conj mws #'refactor/wrap-refactor)
             mw/linearize-middleware-stack
             reverse
             (apply comp))]
    (stack nrepl-server/unknown-op)))

(defn save-port-file
  [port]
  (let [port-file (io/file ".nrepl-port")]
    (.deleteOnExit port-file)
    (spit port-file port)))

(defn run [args]
  (let [port 7004
        app (-> args
                :app
                keyword)]
    (prn "Starting APP: " app)
    (nrepl-server/start-server :port port :handler handler)
    (save-port-file 7004)
    (println (str "\nnRepl started on " port ".\n "))
    (restart)))

(comment
  (run {})

  )
ns user
  (:require [cider.nrepl :as ci]
            [clojure.java.io :as io]
            ;; wellnecity.core
            [libox.v2 :as box]
            [nrepl.middleware :as mw]
            [nrepl.server :as nrepl-server]
            [refactor-nrepl.middleware :as refactor]
            [clojure.string :as str]))

(def app
  {:import {:fhir-4.0.0 {}}
   :version    15})


(defn start
  [app]
  (let [_ (box/start app)
       #_#_ zen-cfgs (zen.cfg/get-configs @manifest)]
    :start-ok))

(defonce dev-server (atom nil))


(defn dev-stop []
  (when @dev-server
    (box/stop @dev-server))
  (reset! dev-server nil))

(defn dev-restart []
  (dev-stop)
  (let [app' (-> app
                 (assoc-in [:config :web :port] 8084))]
    (reset! dev-server (start app')))
  :ok)

(defn restart []
  (prn "Restarting.")
  (dev-restart))

(def handler
  (let [mws
        (->> ci/cider-middleware
             (map resolve)
             (concat nrepl-server/default-middleware))
        stack
        (->> (conj mws #'refactor/wrap-refactor)
             mw/linearize-middleware-stack
             reverse
             (apply comp))]
    (stack nrepl-server/unknown-op)))

(defn save-port-file
  [port]
  (let [port-file (io/file ".nrepl-port")]
    (.deleteOnExit port-file)
    (spit port-file port)))

(defn run [args]
  (let [port 7004
        app (-> args
                :app
                keyword)]
    (prn "Starting APP: " app)
    (nrepl-server/start-server :port port :handler handler)
    (save-port-file 7004)
    (println (str "\nnRepl started on " port ".\n "))
    (restart)))

(comment
  (run {})

  )
