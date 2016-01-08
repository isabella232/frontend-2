(ns frontend.components.builds-table
  (:require [cemerick.url :as url]
            [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]
            [frontend.analytics :as analytics]
            [frontend.async :refer [raise!]]
            [frontend.datetime :as datetime]
            [frontend.components.common :as common]
            [frontend.components.forms :as forms]
            [frontend.components.svg :refer [svg]]
            [frontend.models.build :as build-model]
            [frontend.models.feature :as feature]
            [frontend.utils :as utils :include-macros true]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [frontend.utils :refer [html]]))

(defn build-row [build owner {:keys [show-actions? show-branch? show-project? show-log? show-parallelism?]}]
  (let [url (build-model/path-for (select-keys build [:vcs_url]) build)]
    [:tr {:class (when (:dont_build build) "dont_build")}
     [:td
      [:a {:title (str (:username build) "/" (:reponame build) " #" (:build_num build))
          :href url}
       (when show-project? (str (:username build) "/" (:reponame build) " ")) "#" (:build_num build)]]
     [:td
      (if-not (:vcs_revision build)
        [:a {:href url}]
        [:a {:title (build-model/github-revision build)
             :href url}
         (build-model/github-revision build)])]
     (when show-branch?
       [:td
        [:a
         {:title (build-model/vcs-ref-name build)
          :href url}
         (-> build build-model/vcs-ref-name (utils/trim-middle 23))]])
     [:td.recent-user
      [:a
       {:title (build-model/ui-user build)
        :href url}
       (build-model/author build)]]
     (when show-log?
       [:td.recent-log
        [:a
         {:title (:body build)
          :href url}
         (:subject build)]])
     (if (or (not (:start_time build))
             (= "not_run" (:status build)))
       [:td {:col-span 2}]
       (list [:td.recent-time
              [:a
               {:title  (datetime/full-datetime (js/Date.parse (:start_time build)))
                :href url}
               (om/build common/updating-duration {:start (:start_time build)} {:opts {:formatter datetime/time-ago}})
               " ago"]]
             [:td.recent-time
              [:a
               {:title (build-model/duration build)
                :href url}
               (om/build common/updating-duration {:start (:start_time build)
                                                   :stop (:stop_time build)})]]))
     (when show-parallelism?
       [:td
        (:parallel build) ])
     [:td.recent-status-badge
      [:a
       {:title "status"
        :href url
        :class (build-model/status-class build)}
       (build-model/status-words build)]]
     (when show-actions?
       [:td.build_actions
        (when (build-model/can-cancel? build)
          (let [build-id (build-model/id build)
                vcs-url (:vcs_url build)
                build-num (:build_num build)]
            (forms/managed-button
              [:button.cancel-build
               {:on-click #(raise! owner [:cancel-build-clicked {:build-id build-id
                                                                 :vcs-url vcs-url
                                                                 :build-num build-num}])
                :data-loading-text "Canceling..."
                :data-success-text "Canceled"}
               "Cancel"])))])]))

(defn builds-table-v1 [builds owner {:keys [show-actions? show-branch? show-project? show-log? show-parallelism?]
                                     :or {show-branch? true
                                          show-project? true
                                          show-log? true}}]
  (reify
    om/IDisplayName (display-name [_] "Builds Table V1")
    om/IRender
    (render [_]
      (html
       [:table.recent-builds-table
        [:thead
         [:tr
          [:th "Build"]
          [:th "Revision"]
          (when show-branch?
            [:th "Branch"])
          [:th "Author"]
          (when show-log? [:th "Log"])
          [:th.condense "Started"]
          [:th.condense "Length"]
          (when show-parallelism? [:th "Containers"])
          [:th.condense "Status"]
          (when show-actions?
            [:th.condense "Actions"])]]
        [:tbody
         (map #(build-row % owner {:show-actions? show-actions?
                                   :show-branch? show-branch?
                                   :show-project? show-project?
                                   :show-log? show-log?
                                   :show-parallelism? show-parallelism?})
              builds)]]))))

(defn dashboard-icon [name]
  [:img.dashboard-icon {:src (utils/cdn-path (str "/img/inner/icons/" name ".svg"))}])

(defn build-status-badge-wording [build]
  (let [wording       (build-model/status-words build)
        too-long?     (> (count wording) 10)]
    [:div {:class (if too-long?
                    "badge-text small-text"
                    "badge-text")}
     wording]))

(defn build-status-badge [build]
  [:div.recent-status-badge {:class (build-model/status-class build)}
   (om/build svg {:class "badge-icon"
                  :src (-> build build-model/status-icon-v2 common/icon-path)})
   (build-status-badge-wording build)])

(defn avatar [user & {:keys [size trigger] :or {size 40} :as opts}]
  (if-let [avatar-url (-> user :avatar_url)]
    [:img.dashboard-icon
     ;; Adding `&s=N` to the avatar URL returns an NxN version of the
     ;; avatar (except, for some reason, for default avatars, which are
     ;; always returned full-size, but they're sized with CSS anyhow).
     {:src (-> avatar-url url/url (assoc-in [:query "s"] size) str)}]
    (if (= trigger "api")
      (dashboard-icon "Bot-Icon")
      (dashboard-icon "Default-Avatar"))))

(defn build-row-v2 [build owner {:keys [show-actions? show-branch? show-project?]}]
  (let [url (build-model/path-for (select-keys build [:vcs_url]) build)]
    [:div.build {:class (cond-> [(build-model/status-class build)]
                          (:dont_build build) (conj "dont_build"))}
     [:div.status-area
      [:a {:href url
           :title (build-model/status-words build)}
       (build-status-badge build)]

      (when (build-model/can-cancel? build)
        [:div.build-actions
         (let [build-id (build-model/id build)
               vcs-url (:vcs_url build)
               build-num (:build_num build)]
           (forms/managed-button
            [:button.cancel-build
             {:data-loading-text "Canceling..."
              :on-click #(raise! owner [:cancel-build-clicked {:build-id build-id
                                                               :vcs-url vcs-url
                                                               :build-num build-num}])}
             [:img.cancel-icon {:src (common/icon-path "Status-Canceled")}]
             [:span.cancel-text "Cancel"]]))])]

     [:div.build-info
      [:div.build-info-header
       [:div.contextual-identifier
        [:a {:title (str (:username build) "/" (:reponame build) " #" (:build_num build))
             :href url}

         (when show-project?
           (str (:username build) " / " (:reponame build) " "))

         (when (and show-project? show-branch?) " / ")

         (when show-branch?
           (-> build build-model/vcs-ref-name))
         " #"
         (:build_num build)]]]
      [:div.recent-commit-msg
       (let [pusher-name (build-model/ui-user build)
             trigger (:why build)]
         [:div.recent-user
          {:title (if (= "api" trigger)
                    "API"
                    pusher-name)
           :data-toggle "tooltip"
           :data-placement "right"}
          (avatar (:user build) :trigger trigger)])
       [:span.recent-log
        {:title (:body build)}
        (:subject build)]]]

     [:div.metadata
      [:div.metadata-row.timing
        (if (or (not (:start_time build))
                (= "not_run" (:status build)))
          (list
           [:div.metadata-item.recent-time.start-time
            {:title "Started: not started"}
            "–"]
           [:div.metadata-item.recent-time.duration
            {:title "Duration: not run"}
            "–"])
          (list [:div.metadata-item.recent-time.start-time
                 {:title (str "Started: " (datetime/full-datetime (js/Date.parse (:start_time build))))}
                 (om/build common/updating-duration {:start (:start_time build)} {:opts {:formatter datetime/time-ago-abbreviated}})
                 " ago"]
                [:div.metadata-item.recent-time.duration
                 {:title (str "Duration: " (build-model/duration build))}
                 (om/build common/updating-duration {:start (:start_time build)
                                                     :stop (:stop_time build)})]))]
      [:div.metadata-row.pull-revision
        (when-let [urls (seq (:pull_request_urls build))]
          [:div.metadata-item.pull-requests {:title "Pull Requests"}
           (interpose
            ", "
            (for [url urls]
              [:a {:href url
                   :on-click #(analytics/track "build-card-pr-link-clicked")}
               "#"
               (let [[_ number] (re-find #"/(\d+)$" url)]
                 (or number "?"))]))])

        [:div.metadata-item.revision
         (when (:vcs_revision build)
           [:a {:title (build-model/github-revision build)
                :href (build-model/github-commit-url build)
                :on-click #(analytics/track "build-card-revision-link-clicked")}
            (build-model/github-revision build)])]]]]))

(defn builds-table-v2 [builds owner {:keys [show-actions? show-branch? show-project?]
                                     :or {show-branch? true
                                          show-project? true}}]
  (reify
    om/IDisplayName (display-name [_] "Builds Table V2")
    om/IRender
    (render [_]
      (html
        [:div.container-fluid
         (map #(build-row-v2 % owner {:show-actions? show-actions?
                                      :show-branch? show-branch?
                                      :show-project? show-project?})
              builds)]))))

(defn builds-table [builds owner opts]
  (if (feature/enabled? :ui-v2)
    (builds-table-v2 builds owner opts)
    (builds-table-v1 builds owner opts)))
