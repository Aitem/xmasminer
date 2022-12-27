(ns app.layout
  (:require [zframes.layout :as zl]
            [zframes.styles :as s]
            [zframes.routing :as zr]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [app.pages.index.model :as index]
            [app.auth :as auth]))

(def app-styles
  (s/styles
   []))

(defmethod zl/layout :main
  [req]
  (fn [{page :current-page :as req} resp]
    resp))
