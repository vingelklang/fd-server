-- :name insert-day! :! :n
-- :doc adds the predictions for this day
INSERT INTO predictions
(day, M01, M02, M03)
VALUES (:day, :M01, :M02, :M03)

-- :name get-day :? :1
-- :doc retrieves the predictions for this date
SELECT * FROM predictions
WHERE day = :day
