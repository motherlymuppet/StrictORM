package org.stevenlowes.tools.strictorm.dao

//TODO STEP 1 - Allow for DAO as field
    //TODO STEP 1a - Validation should check that fields are valid types
    //TODO STEP 1b - This includes that they should be registered DAOs
    //TODO STEP 1c - SELECT should do an inner join
    //TODO STEP 1d - What do we do about foreign keys? What about on delete/update? Do we let the user decide? Hopefully not.
    //TODO step 1e - INSERT needs to recursively update children
    //TODO step 1f - How do we tell whether the children have been inserted already? Maybe everything should just use REPLACE instead of separate INSERT / UPDATE

//TODO STEP 2 - Allow for multiple layers of nested DAO

//TODO STEP 3 - Allow DAO A to have DAO B as parameter which has DAO A as parameter
    //TODO STEP 3a. Think this requires table aliases be used

//TODO STEP 4 - MANY-TO-MANY