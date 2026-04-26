# Ancient Media Player Admin Manual

## Purpose

This manual is for the person managing the app content and setup. It explains how to maintain sections such as radio, TV, YouTube, categories, thumbnails, and the home screen layout.

## App Overview

Ancient Media Player is a multi-purpose media application with the following major sections:

- Music
- Radio
- TV
- YouTube
- Videos
- Home screen shortcuts
- Bottom navigation with `More` support

## Content Management Areas

### 1. Radio Management

Radio supports custom user-managed entries.

Admin or manager actions:

- Add radio stations
- Edit radio stations
- Delete radio stations
- Create radio categories
- Edit radio categories
- Delete radio categories
- Assign stations to categories
- Add thumbnails for stations and categories

Required data for a radio station:

- Station name
- Stream URL
- Optional image
- Optional category

### 2. TV Management

TV supports saved streaming channels, including Christian channels and free movie channels.

Admin or manager actions:

- Add TV channels
- Edit TV channels
- Delete TV channels
- Create TV categories
- Edit TV categories
- Delete TV categories
- Assign channels to categories
- Add thumbnails for channels and categories

Suggested channel data:

- Channel name
- Stream or site URL
- Optional image
- Optional category

### 3. YouTube Management

The YouTube section stores channels for quick access.

Admin or manager actions:

- Add YouTube channels
- Edit YouTube channels
- Delete YouTube channels
- Add thumbnails

Suggested channel data:

- Channel name
- Channel URL
- Optional image

### 4. Image Management

Images can be added by:

- Remote image URL
- Upload from device storage

The upload flow supports:

- Crop
- Resize
- Compression for thumbnail use

Recommended practice:

- Use clear square or near-square images where possible.
- Use compressed images for faster loading.
- Keep category artwork visually distinct from item artwork.

### 5. Category Management

Categories are currently important for:

- Radio
- TV

Admin or manager actions:

- Create categories
- Rename categories
- Delete categories
- Add thumbnails for categories
- Assign items into categories

Recommended category approach:

- Keep names short and clear.
- Avoid duplicate category names.
- Use categories such as `Gospel`, `News`, `Talk`, `Live TV`, `Movies & Family`, or `International` where appropriate.

### 6. Home Screen Management

The app supports user-visible home shortcut management.

Admin or manager actions:

- Enable or disable shortcuts/features on the home screen
- Control which items appear on the home page
- Keep the home screen focused on the most important sections

Common home items include:

- History
- Last added
- Most played
- Shuffle
- Choose Folder
- Radio
- TV
- Videos
- YouTube

### 7. Bottom Navigation Management

Because the app contains more features than a standard bottom navigation can hold comfortably, excess sections are surfaced through a `More` tab.

Admin considerations:

- Keep the highest-priority sections in direct navigation where possible.
- Use `More` for lower-frequency destinations.
- Verify navigation after adding new sections or changing visibility rules.

## User Capabilities Summary

End users can:

- Play local music
- Stream radio
- Watch TV channels
- Open saved YouTube channels
- Browse and play local videos
- Use categories for easier organization
- Add or manage artwork
- Personalize the home screen

## Version Information

- App name: Ancient Media Player
- Version code: 1
- Version name: Visualisation

## Recommended Next Step

If these documents are going to be user-facing inside the app, the next step is to wire this content into:

- an in-app `About` screen
- a `Help` or `User Guide` screen
- an admin/settings help page
- Play Store metadata files
