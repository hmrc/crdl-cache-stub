db = connect("mongodb://localhost/crdl-cache");

const ignoredFields = new Set(["_id", "activeFrom", "activeTo"]);

const officeData = db.customsOfficeLists
  .find({
    // Only fetch data that is currently active
    activeFrom: { $lte: new Date() },
    $or: [{ activeTo: { $gt: new Date() } }, { activeTo: { $eq: null } }],
  })
  .sort({ referenceNumber: 1 })
  .toArray();

fs.writeFileSync(
  path.join(".", "conf", "data", "customsOffices.json"),
  EJSON.stringify(
    officeData,
    (key, value) => {
      if (ignoredFields.has(key)) {
        value = undefined;
      }

      // Transform date values to the format accepted by hmrc-mongo's Formats
      if (
        value &&
        value["$date"] !== undefined &&
        // Dates before year 1970 or after year 9999 are already in the expected format
        value["$date"]["$numberLong"] === undefined
      ) {
        const epochMillis = Date.parse(value["$date"]);
        value = {
          $date: { $numberLong: epochMillis.toString() },
        };
      }

      return value;
    },
    2
  )
);
