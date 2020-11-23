-- :name insert-day! :! :n
-- :doc adds the predictions for this day
INSERT INTO predictions
(day, M01, M02, M03, M04)
VALUES (:day, :M01, :M02, :M03, :M04)

-- :name get-by-saved-on :? :1
-- :doc retrieves the predictions by the saved on date
SELECT * FROM predictions
WHERE  day = :day

