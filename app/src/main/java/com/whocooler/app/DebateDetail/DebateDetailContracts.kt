package com.whocooler.app.DebateDetail

import com.whocooler.app.Common.Models.Debate
import com.whocooler.app.Common.Models.Message
import io.reactivex.rxjava3.subjects.PublishSubject

class DebateDetailContracts {

    interface ViewInteractorContract {
        // Functions for View output / Interactor input
        fun initDebate(debate: Debate)
        fun handleSend(text: String, threadId: String? = null, editedMessage: Message? = null, index: Int?=null)
        fun vote(sideId: String) : PublishSubject<Debate>
        fun getNextRepliesPage(parentMessage: Message, index: Int)
    }

    interface InteractorPresenterContract {
        // Functions for Interactor output / Presenter input
        fun presentDebate(debate: Debate)
        fun presentAuthScreen()
        fun presentNewMessage(message: Message)
        fun presentNewReply(threadMessage: Message, index: Int)
        fun updateDebate(debate: Debate)
        fun presentNewRepliesBatch(message: Message, index: Int)
    }

    // Presenter -> View

    interface PresenterViewContract {
        // Functions for Presenter output / View input
        fun displayDebate(rows: ArrayList<DebateDetailAdapter.IRow>)
        fun navigateToAuth()
        fun addNewMessage(row: DebateDetailAdapter.IRow)
        fun addNewReply(reply: Message, index: Int)
        fun resetEditText()
        fun updateMessageCounter(value: Int)
        fun updateDebate(debate: Debate)
        fun addNewRepliesBatch(message: Message, index: Int)
    }

    // Router

    interface RouterInterface {
        fun navigateToAuth()
    }

}