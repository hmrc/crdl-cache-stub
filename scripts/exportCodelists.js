db = connect("mongodb://localhost/crdl-cache");

const ignoredFields = new Set(["_id", "activeFrom", "activeTo", "updatedAt"]);

const codelistData = db.codelists
  .find({
    // Only fetch data that is currently active
    activeFrom: { $lte: new Date() },
    $or: [{ activeTo: { $gt: new Date() } }, { activeTo: { $eq: null } }],
  })
  .sort({ codeListCode: 1, key: 1 })
  .toArray();

const corrlistData = db.correspondenceLists
  .find({
    activeFrom: { $lte: new Date() },
    $or: [{ activeTo: { $gt: new Date() } }, { activeTo: { $eq: null } }],
  })
  .sort({ codeListCode: 1, key: 1, value: 1 })
  .toArray();

fs.writeFileSync(
  path.join(".", "conf", "data", "codelists.json"),
  EJSON.stringify(
    codelistData.concat(corrlistData),
    (key, value) => (ignoredFields.has(key) ? undefined : value),
    2
  )
);
