# StrictORM
A WIP Kotlin ORM that is inflexible but foolproof

Currently everything except many-to-many relationships works.

The ORM works without any annotations, or special handling whatsoever. It uses reflection to inspect the properties of data classes and maps them to SQL.

It was built with the mentality of "You will do things my way, and they will work" as opposed to there being 12 ways to do each thing and all of them being bad in different ways.
