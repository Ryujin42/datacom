module.exports = {
  extends: ["@commitlint/config-conventional"],
  rules: {
    // Types adaptes au projet : les lots (L0..L6) se retrouvent dans le scope, pas le type.
    "type-enum": [
      2,
      "always",
      ["feat", "fix", "test", "docs", "build", "ci", "chore", "refactor", "perf", "style"],
    ],
    "subject-case": [0],
  },
};
