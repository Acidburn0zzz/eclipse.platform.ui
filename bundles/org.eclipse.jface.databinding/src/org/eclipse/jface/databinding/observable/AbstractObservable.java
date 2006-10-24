/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jface.databinding.observable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @since 1.0
 * 
 */
public abstract class AbstractObservable implements IObservable {

	/**
	 * Points to an instance of IChangeListener or a Collection of
	 * IChangeListener
	 */
	private Object changeListeners = null;

	/**
	 * Points to an instance of IChangeListener or a Collection of
	 * IChangeListener
	 */
	private Object staleListeners = null;
	
	private Realm realm;

	/**
	 * @param realm
	 */
	public AbstractObservable(Realm realm) {
		this.realm = realm;
	}

	public void addChangeListener(IChangeListener listener) {
		if (changeListeners == null) {
			boolean hadListeners = hasListeners();
			changeListeners = listener;
			if (!hadListeners) {
				firstListenerAdded();
			}
			return;
		}

		Collection listenerList;
		if (changeListeners instanceof IChangeListener) {
			IChangeListener l = (IChangeListener) changeListeners;

			listenerList = new ArrayList();
			listenerList.add(l);
			changeListeners = listenerList;
		} else {
			listenerList = (Collection) changeListeners;
		}

		listenerList.add(listener);
	}

	public void removeChangeListener(IChangeListener listener) {
		if (changeListeners == listener) {
			changeListeners = null;
			if (!hasListeners()) {
				lastListenerRemoved();
			}
			return;
		}

		if (changeListeners instanceof Collection) {
			Collection listenerList = (Collection) changeListeners;
			listenerList.remove(listener);
			if (listenerList.isEmpty()) {
				changeListeners = null;
				if (!hasListeners()) {
					lastListenerRemoved();
				}
			}
		}
	}

	public void addStaleListener(IStaleListener listener) {
		if (staleListeners == null) {
			boolean hadListeners = hasListeners();
			staleListeners = listener;
			if (!hadListeners) {
				firstListenerAdded();
			}
			return;
		}

		Collection listenerList;
		if (staleListeners instanceof IStaleListener) {
			IStaleListener l = (IStaleListener) staleListeners;

			listenerList = new ArrayList();
			listenerList.add(l);
			staleListeners = listenerList;
		} else {
			listenerList = (Collection) staleListeners;
		}

		listenerList.add(listener);
	}

	public void removeStaleListener(IStaleListener listener) {
		if (staleListeners == listener) {
			staleListeners = null;
			if (!hasListeners()) {
				lastListenerRemoved();
			}
			return;
		}

		if (staleListeners instanceof Collection) {
			Collection listenerList = (Collection) staleListeners;
			listenerList.remove(listener);
			if (listenerList.isEmpty()) {
				staleListeners = null;
				if (!hasListeners()) {
					lastListenerRemoved();
				}
			}
		}
	}

	protected void fireChange() {
		if (changeListeners == null) {
			return;
		}

		if (changeListeners instanceof IChangeListener) {
			((IChangeListener) changeListeners).handleChange(this);
			return;
		}

		Collection changeListenerCollection = (Collection) changeListeners;

		IChangeListener[] listeners = (IChangeListener[]) (changeListenerCollection)
				.toArray(new IChangeListener[changeListenerCollection.size()]);
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].handleChange(this);
		}
	}

	protected void fireStale() {
		if (staleListeners == null) {
			return;
		}

		if (staleListeners instanceof IStaleListener) {
			((IStaleListener) staleListeners).handleStale(this);
			return;
		}

		Collection staleListenerCollection = (Collection) staleListeners;

		IStaleListener[] listeners = (IStaleListener[]) (staleListenerCollection)
				.toArray(new IStaleListener[staleListenerCollection.size()]);
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].handleStale(this);
		}
	}

	/**
	 * @return true if this observable has listeners
	 */
	protected boolean hasListeners() {
		return changeListeners != null || staleListeners != null;
	}

	/**
	 * 
	 */
	protected void firstListenerAdded() {
	}

	/**
	 * 
	 */
	protected void lastListenerRemoved() {
	}

	/**
	 * 
	 */
	public void dispose() {
		changeListeners = null;
		staleListeners = null;
	}

	/**
	 * @return Returns the realm.
	 */
	public Realm getRealm() {
		return realm;
	}

}
