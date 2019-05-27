/*
 * Copyright (c) 2017 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.cheesefinder

import android.text.Editable
import android.text.TextWatcher
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_cheeses.*
import java.util.concurrent.TimeUnit

class CheeseActivity : BaseSearchActivity() {

    override fun onStart() {
        super.onStart()

        val buttonClickStream = createButtonClickObservable()
        val textChangeStream = createTextChangeObservable()

        // Merge the two observables into one that emits a String
        val searchTextObservable = Observable.merge<String>(buttonClickStream, textChangeStream)

        // Subscribe to the Observable with subscribe() and supply a simple Consumer
        searchTextObservable
                // The next operator in chain will be run on main thread
                .observeOn(AndroidSchedulers.mainThread())
                // showProgress() will be called every time a new item is emitted
                .doOnNext { showProgress() }
                // Next operator should be called on I/O thread
                .observeOn(Schedulers.io())
                // For each search query, return a list of results
                .map { cheeseSearchEngine.search(it) }
                // Results are passed to the list on main thread
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    hideProgress()
                    showResult(it)
                }
    }

    // This function returns an Observable for text changes
    private fun createTextChangeObservable(): Observable<String> {

        // Create an Observable which takes an ObservableOnSubscribe
        val textChangeObservable = Observable.create<String> { emitter ->

            // When observer subscribes, make a TextWatcher
            val textWatcher = object : TextWatcher {

                override fun afterTextChanged(s: Editable?) = Unit

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

                // When user types and this triggers, you pass new text value to observer
                override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    s?.toString()?.let { emitter.onNext(it) }
                }
            }

            // Add the watcher to your TextView by calling addTextChangedListener
            queryEditText.addTextChangedListener(textWatcher)

            // Remove your watcher
            emitter.setCancellable {
                queryEditText.removeTextChangedListener(textWatcher)
            }
        }

        // Remove the created observable
        return textChangeObservable
                .filter { it.length >= 2 }
                // debounce will wait for a specified amount of time after each item for ANOTHER item
                // If no item happens to be emitted, the last item is finally emitted
                // debounce waits 1 second before emitting the latest query text
                .debounce(1000, TimeUnit.MILLISECONDS)
    }

    // This function will return an Observable that will emit Strings
    private fun createButtonClickObservable(): Observable<String> {
        // Create the Observable and provide is an ObservableOnSubscribe
        return Observable.create { emitter ->

            // Setup the listener on the search button
            searchButton.setOnClickListener {

                // Pass the EditText content to the emitter
                emitter.onNext(queryEditText.text.toString())
            }

            // Will be called when Observable is disposed (i.e. Observable is completed or unsubscribed)
            emitter.setCancellable {
                searchButton.setOnClickListener(null)
            }
        }
    }
}