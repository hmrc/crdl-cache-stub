db = connect("mongodb://localhost/crdl-cache");

const ignoredFields = new Set(["_id"]);

const lastUpdatedData = db["last-updated"]
  .find()
  .sort({ codeListCode: 1 })
  .toArray();

fs.writeFileSync(
  path.join(".", "conf", "data", "lastUpdated.json"),
  EJSON.stringify(
    lastUpdatedData,
    (key, value) => {
      if (ignoredFields.has(key)) {
        value = undefined;
      }

      // Transform date values from extended JSON to string values
      if (value && value["$date"] !== undefined) {
        value = value["$date"];
      }

      return value;
    },
    2
  )
);
