(ns frontend.components.docs.time-day)

(def article
  {:title "Time-based results are one day off"
   :url :time-day
   :last-updated "Feb 3, 2013"
   :content [:div
             [:p
              "Time-zones are fickle masters, and time-based code frequently only works in the time-zone in which it was written.Your code is probably tested in local time on your development machine, while CircleCI is in UTC.If that's the case, you may end up with test failures looking like this:"]
             [:pre
              "’‘"
              [:code.no-highlight
               "’1) 1-day trial should be 1 day long   Failure/Error: @trial.expiration.should == 14.day.from_now.to_date   expected: Fri, 28 May 2012        got: Sat, 29 May 2012 (using ==)‘"]
              "’‘"]
             [:p
              "Fortunately, it's easy to"
              [:a
               {:href "/docs/configuration#timezone"}
               "change CircleCI's timezone"]
              "to match yours."]
             [:p
              "When making this change, it might be a good time to check what time zone your server is in.You should set CircleCI to use the same timezone as the servers to which you deploy, not the same time zones as your development machines.Otherwise you might be in for a nasty surprise when your users get a 0-day trial, but it works fine on your machine!"]
             [:h3 "Daylight savings time"]
             [:p
              "If this doesn't fix the issues, check if we're near a Daylight Savings Time transition.Your code won't be the first, nor will it be the last, to not properly deal with time zones.A common problem is that one part of the time interval you're measuring will be in Daylight savings time, and the other won't, creating a one hour discrepancy.This leads to a 2 hour window in which your tests might fail."]
             [:p
              "One nice solution is to just try again, preferably at a different time of day.Try to avoid midnight in both your local timezone, and the time zone that CircleCI uses (UTC unless you"
              [:a {:href "/docs/configuration#timezone"} "changed"]
              "it)."]]})
