-- :name insert-day! :! :n
-- :doc adds the predictions for this day
INSERT INTO predictions
(day, M01, M02, M03, M04)
VALUES (:day, :M01, :M02, :M03, :M04);

-- :name get-by-saved-on :? :1
-- :doc retrieves the predictions by the saved on date
SELECT * FROM predictions
WHERE  day = :day;

-- :name get-latest :? :1
-- :doc Latest recorded row by date
SELECT * FROM predictions
ORDER BY day DESC LIMIT 1;

-- :name get-all-predictions :? :*
-- :doc Returns all values from the predictions table
SELECT * FROM predictions;