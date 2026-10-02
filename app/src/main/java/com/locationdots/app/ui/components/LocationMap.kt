package com.locationdots.app.ui.components

import android.graphics.Color
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geojson.Feature
import org.maplibre.android.geojson.FeatureCollection
import org.maplibre.android.geojson.LineString
import org.maplibre.android.geojson.Point
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val ROUTE_SOURCE = "location-dots-route"
private const val POINT_SOURCE = "location-dots-points"
private const val ROUTE_LAYER = "location-dots-route-layer"
private const val POINT_LAYER = "location-dots-point-layer"

@Composable
fun LocationMap(
    points: List<LatLng>,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    fitRequest: Any? = points
) {
    if (points.isEmpty()) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { readyMap ->
                    map = readyMap
                }
            }
        },
        update = { view ->
            view.getMapAsync { readyMap ->
                map = readyMap
            }
        }
    )

    LaunchedEffect(map, points, interactive, fitRequest) {
        val readyMap = map ?: return@LaunchedEffect
        readyMap.uiSettings.setAllGesturesEnabled(interactive)
        readyMap.uiSettings.setLogoEnabled(true)
        readyMap.setStyle(
            Style.Builder().fromUri(OPEN_FREE_MAP_STYLE)
        ) { style ->
            val route = points
                .takeIf { it.size >= 2 }
                ?.let { coordinates ->
                    GeoJsonSource(
                        ROUTE_SOURCE,
                        Feature.fromGeometry(
                            LineString.fromLngLats(
                                coordinates.map { Point.fromLngLat(it.longitude, it.latitude) }
                            )
                        )
                    )
                }

            val pointFeatures = FeatureCollection.fromFeatures(
                points.map { coordinate ->
                    Feature.fromGeometry(Point.fromLngLat(coordinate.longitude, coordinate.latitude))
                }.toTypedArray()
            )

            route?.let {
                style.addSource(it)
                style.addLayer(
                    LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                        lineColor(Color.parseColor("#6750A4")),
                        lineWidth(5f)
                    )
                )
            }

            style.addSource(GeoJsonSource(POINT_SOURCE, pointFeatures))
            style.addLayer(
                CircleLayer(POINT_LAYER, POINT_SOURCE).withProperties(
                    circleColor(Color.parseColor("#6750A4")),
                    circleRadius(7f),
                    circleStrokeColor(Color.WHITE),
                    circleStrokeWidth(2.5f)
                )
            )

            mapView.post {
                val bounds = org.maplibre.android.geometry.LatLngBounds.Builder().apply {
                    points.forEach { include(it) }
                }.build()

                if (points.size == 1) {
                    readyMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(points.first(), 14.0)
                    )
                } else {
                    readyMap.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, 56)
                    )
                }
            }
        }
    }
}
