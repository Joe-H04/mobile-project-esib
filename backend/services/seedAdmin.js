const bcrypt = require("bcryptjs");
const { v4: uuidv4 } = require("uuid");
const store = require("../data/store");

const ADMIN_EMAIL = "admin@betnow.local";
const ADMIN_DEFAULT_PASSWORD = "admin123";

function seedAdmin() {
  let admin = store.users.find((u) => u.email === ADMIN_EMAIL);
  if (!admin) {
    admin = {
      id: uuidv4(),
      email: ADMIN_EMAIL,
      passwordHash: bcrypt.hashSync(ADMIN_DEFAULT_PASSWORD, 10),
      balance: 0,
      role: "admin",
      createdAt: new Date().toISOString(),
    };
    store.users.push(admin);
    store.persist();
    console.log(
      `Seeded admin user: ${ADMIN_EMAIL} (password: ${ADMIN_DEFAULT_PASSWORD})`
    );
  } else if (admin.role !== "admin") {
    admin.role = "admin";
    store.persist();
    console.log(`Upgraded ${ADMIN_EMAIL} to admin role`);
  }
}

module.exports = { seedAdmin, ADMIN_EMAIL };
