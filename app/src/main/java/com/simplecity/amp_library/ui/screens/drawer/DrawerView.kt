package com.simplecity.amp_library.ui.screens.drawer

import com.simplecity.amp_library.model.Playlist
import com.simplecity.amp_library.ui.screens.playlist.menu.PlaylistMenuContract

interface DrawerView :
    PlaylistMenuContract.View {

    fun setPlaylistItems(playlists: List<Playlist>)

    fun closeDrawer()

    fun setDrawerItemSelected(@DrawerParent.Type type: Int)
}