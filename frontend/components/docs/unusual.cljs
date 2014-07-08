(ns frontend.components.docs.unusual)

(def article
  {:title "Can I test projects with unusual requirements?"
   :last-updated "April 29, 2013"
   :url :unusual
   :content [:div
             [:p
              "CircleCI is completely configurable.We have a simple setup for projects that follow established conventions, but we also support many variations on the theme."]
             [:ul
              [:li
               "You can"
               [:a
                {:href "\\/docs/configuration#subdirectory\\"}
                " run tests from a subdirectory of your repository"]
               "."]
              [:li
               "You can"
               [:a
                {:href "/docs/configuration"}
                "override every single phase of your tests"]
               "with custom settings or minor tweaks."]
              [:li
               "Though you'll rarely need it, you can tweak your code to"
               [:a {:href "/docs/dont-run"} "avoid certain behaviours"]
               "during your CI builds."]]]})
