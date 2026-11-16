# SoundCloud
- Source: a SoundCloud redesign on Dribbble that Arch found and asked for. Free to use, according to its maker (as Arch reported it). Photoshop file: https://cdn.dribbble.com/users/52334/screenshots/625946/attachments/52192/Soundcloud.psd (Arch's preview picture: /mnt/user-data/uploads/610e4dde-image.jpg).
- What it is: a yellow-orange half circle, two bars that get taller, then a half cloud. The colours go from yellow-orange on the left to SoundCloud orange on the right. This replaces version 4 (four bars and a cloud, all in one orange) and the older plan in app_notes.json, because Arch picked this design.
- How it was made: copied from the Photoshop file, not traced or redrawn. The "Large" version of the logo in that file is made of 4 vector shapes. Their exact outlines (curves) and fill colours were turned straight into the SVG, at the same positions. The file has no gradients or effects, only flat colours:
  - half circle #FFAF1A, short bar #FA931D, tall bar #F67621, cloud #FF5500 (SoundCloud's own orange).
- Checked: the SVG was drawn and compared pixel by pixel with Photoshop's own drawing of the same logo. Every solid pixel has exactly the same colour. Only the soft edge pixels differ a little (at most 35 out of 255, usually less than 1), because every program smooths edges slightly differently. The shapes are the same size to within 0.3%, and nothing is shifted.
- Background is see-through; nothing was added or left out.
- Preview checked big and at 48 px on the wallpaper and on the app drawer: all four parts stay separate and easy to see on both.
