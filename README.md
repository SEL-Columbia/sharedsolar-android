# [SharedSolar](http://sharedsolar.org) Android App
## [Sustainable Engineering Lab](http://sel.columbia.edu/) at [Columbia University](http://columbia.edu/)

[Download from Google Play](https://play.google.com/store/apps/details?id=org.sharedsolar)

## Description

An Android app for [SharedSolar](http://sharedsolar.org) field technicians and vendors, to control circuits, sell credits, and manage user accounts.

## Development

 * To change the version name, edit <tt>android:versionName</tt> in the [AndroidManifest.xml](AndroidManifest.xml#L3) file.

 * To switch settings between Mali and Uganda, edit the denomination section of the [/res/values/config.xml](res/values/config.xml#L23) file.

 * For creating a release version, set a password in the <tt>init()</tt> function in 
[org.sharedsolar.db.DatabaseAdapter.java](src/org/sharedsolar/db/DatabaseAdapter.java#L67) and rebuild.

 * Use the [sharedsolar-simulator](https://github.com/SEL-Columbia/sharedsolar-simulator) for further app development in the absence of smart meter and mini-grid hardware.