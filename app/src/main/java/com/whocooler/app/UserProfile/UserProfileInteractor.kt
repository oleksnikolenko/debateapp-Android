package com.whocooler.app.UserProfile

import com.facebook.login.LoginManager
import com.whocooler.app.Common.App.App
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import io.reactivex.rxjava3.kotlin.subscribeBy

class UserProfileInteractor: UserProfileContracts.ViewInteractorContract {

    var presenter: UserProfileContracts.InteractorPresenterContract? = null
    var worker = UserProfileWorker()

    override fun getProfile() {
        val user = App.prefs.userSession?.user
        if (user != null) {
            presenter?.presentProfile(user)
        }
    }

    override fun logout() {
        Firebase.auth.signOut()
        LoginManager.getInstance().logOut()
        App.prefs.userSession = null
        presenter?.navigateToAuth()
    }

    override fun updateUserName(newName: String) {
        worker.updateUserName(newName).subscribeBy(
            onNext = {response->
                App.prefs.userSession?.user = response.user
                presenter?.presentProfile(response.user)
            }, onError = {
                presenter?.presentError()
            }
        )
    }

    override fun updateUserAvatar(image: ByteArray) {
        worker.updateUserAvatar(image).subscribeBy(
            onNext = { response ->
                App.prefs.userSession?.user = response.user
                presenter?.presentProfile(response.user)
            }, onError = {
                presenter?.presentError()
            }
        )
    }


}