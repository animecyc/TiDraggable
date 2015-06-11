/**
 * An enhanced fork of the original TiDraggable module by Pedro Enrique,
 * allows for simple creation of "draggable" views.
 *
 * Copyright (C) 2013 Seth Benjamin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * -- Original License --
 *
 * Copyright 2012 Pedro Enrique
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ti.draggable;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;

public class DraggableGesture implements OnTouchListener
{
	protected TiUIView draggableView;
	protected TiViewProxy draggableProxy;
	protected WeakReference<ConfigProxy> config;
	protected VelocityTracker velocityTracker;
	protected ViewConfiguration vc;
	protected double lastX = 0;
	protected double lastY = 0;
	protected double distanceX = 0;
	protected double distanceY = 0;
	protected double startLeft = 0;
	protected double startTop = 0;
	protected double lastLeft;
	protected double lastTop;
	public boolean isBeingDragged = false;

	public DraggableGesture(TiViewProxy proxy, TiUIView view, WeakReference<ConfigProxy> config)
	{
		this.draggableProxy = proxy;
		this.draggableView = view;
		this.vc = ViewConfiguration.get(this.draggableView.getOuterView().getContext());
		this.config = config;

		ConfigProxy config_ = this.getConfig();

		if(config_ != null) {
			config_.threshold = vc.getScaledPagingTouchSlop();
		}

		this.prepareMappedProxies();
	}

	public void determineDrag(MotionEvent event)
	{
		ConfigProxy config = this.getConfig();
		KrollDict configProps = config.getProperties();

		Object thresholdObj = config.getProperty("threshold");
		int threshold = thresholdObj != null ? TiConvert.toInt(thresholdObj) : vc.getScaledPagingTouchSlop();

		boolean xAxis = ! configProps.isNull("axis") && configProps.getString("axis").equals("x");
		boolean yAxis = ! configProps.isNull("axis") && configProps.getString("axis").equals("y");

		double xDelta = Math.abs(event.getRawX() - this.lastX);
		double yDelta = Math.abs(event.getRawY() - this.lastY);

		if(xAxis) {
			this.isBeingDragged = xDelta > threshold;
		}
		else if(yAxis)
		{
			this.isBeingDragged = yDelta > threshold;
		}
		else
		{
			this.isBeingDragged = xDelta > threshold || yDelta > threshold;
		}
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		ConfigProxy config = this.getConfig();

		if (! this.isBeingDragged && event.getAction() == MotionEvent.ACTION_MOVE)
		{
			this.determineDrag(event);
		}

		if (! this.isBeingDragged) {
			return false;
		}

		if (this.velocityTracker != null)
		{
			this.velocityTracker.addMovement(event);
		}

		switch(event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				this.startDrag(event);
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				this.stopDrag(event);
				break;
			case MotionEvent.ACTION_MOVE:
				if (TiConvert.toBoolean(config.getProperty("enabled")) == false)
				{
					return false;
				}

				this.drag(event);
				break;
		}

		return true;
	}

	public void drag(MotionEvent event)
	{
		ConfigProxy config = this.getConfig();
		KrollDict configProps = config.getProperties();
		View viewToDrag = this.draggableView.getOuterView();

		float screenX = event.getRawX();
		float screenY = event.getRawY();

		if (this.isBeingDragged)
		{
			boolean xAxis = ! configProps.isNull("axis") && configProps.getString("axis").equals("x");
			boolean yAxis = ! configProps.isNull("axis") && configProps.getString("axis").equals("y");
			boolean noAxis = ! xAxis && ! yAxis;

			double leftEdge = screenX + startLeft;
			double topEdge = screenY + startTop;

			if (xAxis)
			{
				topEdge = viewToDrag.getTop();
			}
			else if (yAxis)
			{
				leftEdge = viewToDrag.getLeft();
			}

			if (noAxis || xAxis)
			{
				if (! configProps.isNull("minLeft"))
				{
					double _minLeft = config.getDimensionAsPixels("minLeft");

					if (leftEdge <= _minLeft)
					{
						leftEdge = _minLeft;
					}
				}

				if (! configProps.isNull("maxLeft"))
				{
					double _maxLeft = config.getDimensionAsPixels("maxLeft");

					if (leftEdge >= _maxLeft)
					{
						leftEdge = _maxLeft;
					}
				}
			}

			if (noAxis || yAxis)
			{
				if (! configProps.isNull("minTop"))
				{
					double _minTop = config.getDimensionAsPixels("minTop");

					if (topEdge <= _minTop)
					{
						topEdge = _minTop;
					}
				}

				if (! configProps.isNull("maxTop"))
				{
					double _maxTop = config.getDimensionAsPixels("maxTop");

					if (topEdge >= _maxTop)
					{
						topEdge = _maxTop;
					}
				}
			}

			double translationLeft = lastLeft - leftEdge;
			double translationTop = lastTop - topEdge;

			translateMappedProxies(translationLeft, translationTop);

			Float ensureRightValue = null;
			Float ensureBottomValue = null;

			if (TiConvert.toBoolean(configProps, "ensureRight"))
			{
				ensureRightValue = (float) -leftEdge;
			}

			if (TiConvert.toBoolean(configProps, "ensureBottom"))
			{
				ensureBottomValue = (float) -topEdge;
			}

			this.setViewPosition(draggableProxy, viewToDrag, (float) topEdge, (float) leftEdge, ensureBottomValue, ensureRightValue);

			distanceX += Math.abs(lastLeft - leftEdge);
			distanceY += Math.abs(lastTop - topEdge);
			lastLeft = leftEdge;
			lastTop = topEdge;

			if (draggableProxy.hasListeners("move"))
			{
				this.velocityTracker.computeCurrentVelocity(1000);

				KrollDict eventDict = new KrollDict();
				KrollDict velocityDict = new KrollDict();

				velocityDict.put("x", this.velocityTracker.getXVelocity());
				velocityDict.put("y", this.velocityTracker.getYVelocity());
				eventDict.put("left", viewToDrag.getLeft());
				eventDict.put("top", viewToDrag.getTop());
				eventDict.put("velocity", velocityDict);

				draggableProxy.fireEvent("move", eventDict);
			}
		}
	}

	public void startDrag(MotionEvent event)
	{
		View viewToDrag = this.draggableView.getOuterView();

		this.lastX = event.getRawX();
		this.lastY = event.getRawY();

		float screenX = event.getRawX();
		float screenY = event.getRawY();

		this.startLeft = viewToDrag.getLeft() - screenX;
		this.startTop = viewToDrag.getTop() - screenY;
		this.lastLeft = viewToDrag.getLeft();
		this.lastTop = viewToDrag.getTop();
		this.distanceX = this.distanceY = 0;

		if (this.velocityTracker == null)
		{
			this.velocityTracker = VelocityTracker.obtain();
		}

		if (draggableProxy.hasListeners("start"))
		{
			this.velocityTracker.computeCurrentVelocity(1000);

			KrollDict velocityDict = new KrollDict();

			velocityDict.put("x", this.velocityTracker.getXVelocity());
			velocityDict.put("y", this.velocityTracker.getYVelocity());

			KrollDict eventDict = new KrollDict();

			eventDict.put("left", viewToDrag.getLeft());
			eventDict.put("top", viewToDrag.getTop());
			eventDict.put("velocity", velocityDict);

			draggableProxy.fireEvent("start", eventDict);
		}
	}

	public void stopDrag(MotionEvent event)
	{
		if (this.isBeingDragged)
		{
			this.isBeingDragged = false;

			this.finalizeMappedTranslations();

			if (draggableProxy.hasListeners("end") || draggableProxy.hasListeners("cancel"))
			{
				View viewToDrag = this.draggableView.getOuterView();

				this.velocityTracker.computeCurrentVelocity(1000);

				KrollDict distanceDict = new KrollDict();

				distanceDict.put("x", distanceX);
				distanceDict.put("y", distanceY);

				KrollDict velocityDict = new KrollDict();

				velocityDict.put("x", this.velocityTracker.getXVelocity());
				velocityDict.put("y", this.velocityTracker.getYVelocity());

				KrollDict eventDict = new KrollDict();

				eventDict.put("left", viewToDrag.getLeft());
				eventDict.put("top", viewToDrag.getTop());
				eventDict.put("velocity", velocityDict);
				eventDict.put("distance", distanceDict);

				draggableProxy.fireEvent(event.getAction() == MotionEvent.ACTION_UP ? "end" : "cancel", eventDict);
			}

			this.velocityTracker.clear();
		}
	}

	protected void prepareMappedProxies()
	{
		KrollDict configProps = this.getConfig().getProperties();

		if (configProps.containsKeyAndNotNull("maps"))
		{
			for (Object mapObject : (Object[]) configProps.get("maps"))
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				KrollDict map = new KrollDict((HashMap) mapObject);
				TiViewProxy mappedProxy = (TiViewProxy) map.get("view");
				View mappedView = mappedProxy.peekView().getOuterView();

				if (map.containsKeyAndNotNull("constrain"))
				{
					View parentView = this.getConfig().getDecorView();
					KrollDict constraints = map.getKrollDict("constrain");
					KrollDict constraintX = constraints.getKrollDict("x");
					KrollDict constraintY = constraints.getKrollDict("y");

					boolean didModifyPosition = false;

					double parallaxAmount = map.containsKeyAndNotNull("parallaxAmount") ? TiConvert.toDouble(map, "parallaxAmount") : 1;
					double newLeft = 0;
					double newTop = 0;

					if (constraintX != null && constraintX.containsKeyAndNotNull("start"))
					{
						double xStart = TiConvert.toTiDimension(constraintX, "start", TiDimension.TYPE_LEFT).getAsPixels(parentView);

						if (constraintX.containsKeyAndNotNull("end"))
						{
							double xEnd = TiConvert.toTiDimension(constraintX, "end", TiDimension.TYPE_LEFT).getAsPixels(parentView);

							newLeft = xStart / parallaxAmount - xEnd;
						}
						else
						{
							newLeft = mappedView.getLeft() / parallaxAmount;
						}

						didModifyPosition = true;
					}

					if (constraintY != null && constraintY.containsKeyAndNotNull("start"))
					{
						double yStart = TiConvert.toTiDimension(constraintX, "start", TiDimension.TYPE_TOP).getAsPixels(parentView);

						if (constraintY.containsKeyAndNotNull("end"))
						{
							double yEnd = TiConvert.toTiDimension(constraintX, "end", TiDimension.TYPE_TOP).getAsPixels(parentView);

							newTop = yStart / parallaxAmount - yEnd;
						}
						else
						{
							newTop = mappedView.getTop() / parallaxAmount;
						}

						didModifyPosition = true;
					}

					if (didModifyPosition)
					{
						this.setViewPosition(mappedProxy, mappedView, (float) newTop, (float) newLeft, null, null);
					}
				}
			}
		}
	}

	protected void translateMappedProxies(double translationX, double translationY)
	{
		KrollDict configProps = this.getConfig().getProperties();

		if (configProps.containsKeyAndNotNull("maps"))
		{
			for (Object mapObject : (Object[]) configProps.get("maps"))
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				KrollDict map = new KrollDict((HashMap) mapObject);
				TiViewProxy mappedProxy = (TiViewProxy) map.get("view");
				View mappedView = mappedProxy.peekView().getOuterView();
				double parallaxAmount = map.containsKeyAndNotNull("parallaxAmount") ? TiConvert.toDouble(map, "parallaxAmount") : 1;
				
				double newTranslationX = mappedView.getTranslationX() - translationX / parallaxAmount;
				double newTranslationY = mappedView.getTranslationY() - translationY / parallaxAmount;

				mappedView.setTranslationX((float) newTranslationX);
				mappedView.setTranslationY((float) newTranslationY);
			}
		}
	}

	protected void finalizeMappedTranslations()
	{
		KrollDict configProps = this.getConfig().getProperties();

		if (configProps.containsKeyAndNotNull("maps"))
		{
			for (Object mapObject : (Object[]) configProps.get("maps"))
			{
				@SuppressWarnings({ "rawtypes", "unchecked" })
				KrollDict map = new KrollDict((HashMap) mapObject);
				TiViewProxy mappedProxy = (TiViewProxy) map.get("view");
				View mappedView = mappedProxy.peekView().getOuterView();

				this.setViewPosition(mappedProxy, mappedView, mappedView.getY(), mappedView.getX(), null, null);
			}
		}
	}

	protected void setViewPosition(KrollProxy proxy, View view, Float top, Float left, Float bottom, Float right)
	{
		TiCompositeLayout.LayoutParams layout = (TiCompositeLayout.LayoutParams) view.getLayoutParams();

		layout.optionLeft = new TiDimension(left, TiDimension.TYPE_LEFT);
		layout.optionTop = new TiDimension(top, TiDimension.TYPE_TOP);

		if (right != null)
		{
			layout.optionRight = new TiDimension(right, TiDimension.TYPE_RIGHT);
		}

		if (bottom != null)
		{
			layout.optionBottom = new TiDimension(bottom, TiDimension.TYPE_BOTTOM);
		}

		view.setLayoutParams(layout);
		view.setTranslationX(0);
		view.setTranslationY(0);

		proxy.setProperty("left", left);
		proxy.setProperty("top", top);

		if (right != null)
		{
			proxy.setProperty("right", right);
		}

		if (bottom != null)
		{
			proxy.setProperty("bottom", bottom);
		}
	}

	protected ConfigProxy getConfig()
	{
		return this.config.get();
	}
}
