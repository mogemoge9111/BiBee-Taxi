package com.example.bibeetaxi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.VisibleRegionUtils;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapFragment extends Fragment implements Session.SearchListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MapView mapView;
    private EditText etSearch;
    private Button btnSearch, btnSetPickup, btnSetDestination, btnCalculatePrice, btnSaveData, btnCreateRequest;
    private TextView tvPriceInfo;
    private SearchManager searchManager;
    private Session searchSession;
    private MapObjectCollection mapObjects;
    private FirebaseFirestore db;
    private ListenerRegistration driversListener;

    private Point pickupPoint, destinationPoint;
    private PlacemarkMapObject pickupPlacemark, destinationPlacemark;
    private PolylineMapObject currentRoutePolyline;
    private String pickupAddress, destinationAddress;
    private int calculatedPrice = 0;
    private double calculatedDistance = 0;

    private String savedFrom, savedTo;
    private int savedPrice = 0;

    private final List<PlacemarkMapObject> driverPlacemarks = new ArrayList<>();
    private boolean searchModeFrom = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MapKitFactory.getInstance().onStart();
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.mapview);
        etSearch = view.findViewById(R.id.etSearch);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnSetPickup = view.findViewById(R.id.btnSetPickup);
        btnSetDestination = view.findViewById(R.id.btnSetDestination);
        btnCalculatePrice = view.findViewById(R.id.btnCalculatePrice);
        btnSaveData = view.findViewById(R.id.btnSaveData);
        btnCreateRequest = view.findViewById(R.id.btnCreateRequest);
        tvPriceInfo = view.findViewById(R.id.tvPriceInfo);

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        db = FirebaseFirestore.getInstance();
        mapObjects = mapView.getMap().getMapObjects();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        mapView.getMap().move(
                new CameraPosition(new Point(55.751244, 37.618423), 11.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, 0),
                null);

        btnSetPickup.setOnClickListener(v -> {
            searchModeFrom = true;
            etSearch.setHint("Откуда едем?");
            btnSetPickup.setSelected(true);
            btnSetDestination.setSelected(false);
            Toast.makeText(getContext(), "Введите адрес и нажмите 'Найти'", Toast.LENGTH_SHORT).show();
        });

        btnSetDestination.setOnClickListener(v -> {
            searchModeFrom = false;
            etSearch.setHint("Куда едем?");
            btnSetDestination.setSelected(true);
            btnSetPickup.setSelected(false);
            Toast.makeText(getContext(), "Введите адрес и нажмите 'Найти'", Toast.LENGTH_SHORT).show();
        });

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) submitSearch(query);
            else Toast.makeText(getContext(), "Введите адрес", Toast.LENGTH_SHORT).show();
        });

        btnCalculatePrice.setOnClickListener(v -> {
            if (pickupPoint != null && destinationPoint != null) {
                calculatedDistance = haversineDistance(pickupPoint, destinationPoint);
                calculatedPrice = calculatePrice(calculatedDistance);
                int minutes = (int) (calculatedDistance * 3);
                String info = String.format(Locale.getDefault(),
                        "Расстояние: %.1f км\nСтоимость: %d ₽\nПримерное время: %d мин",
                        calculatedDistance, calculatedPrice, minutes);
                tvPriceInfo.setText(info);
            } else {
                Toast.makeText(getContext(), "Сначала установите точки отправления и назначения", Toast.LENGTH_SHORT).show();
            }
        });

        btnSaveData.setOnClickListener(v -> {
            if (pickupPoint == null || destinationPoint == null) {
                Toast.makeText(getContext(), "Сначала установите обе точки и рассчитайте стоимость", Toast.LENGTH_SHORT).show();
                return;
            }
            if (calculatedPrice == 0) {
                calculatedDistance = haversineDistance(pickupPoint, destinationPoint);
                calculatedPrice = calculatePrice(calculatedDistance);
                tvPriceInfo.setText(String.format(Locale.getDefault(),
                        "Расстояние: %.1f км\nСтоимость: %d ₽",
                        calculatedDistance, calculatedPrice));
            }
            savedFrom = pickupAddress != null ? pickupAddress : "";
            savedTo = destinationAddress != null ? destinationAddress : "";
            savedPrice = calculatedPrice;
            Toast.makeText(getContext(), "Данные сохранены для заказа", Toast.LENGTH_SHORT).show();
        });

        btnCreateRequest.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateRideRequestActivity.class);
            intent.putExtra("fromAddress", savedFrom != null ? savedFrom : "");
            intent.putExtra("toAddress", savedTo != null ? savedTo : "");
            intent.putExtra("maxPrice", savedPrice);
            intent.putExtra("city", "");
            startActivity(intent);
        });

        loadAvailableDrivers();
        return view;
    }

    private void setPickupPoint(Point point) {
        pickupPoint = point;
        if (pickupPlacemark != null) mapObjects.remove(pickupPlacemark);
        pickupPlacemark = mapObjects.addPlacemark(point);
        pickupPlacemark.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.marker));
        IconStyle style = new IconStyle();
        style.setScale(0.1f);  // маленький размер
        pickupPlacemark.setIconStyle(style);
        pickupPlacemark.setText("Отправление");
        reverseGeocodeForAddress(point, true);
    }

    private void setDestinationPoint(Point point) {
        destinationPoint = point;
        if (destinationPlacemark != null) mapObjects.remove(destinationPlacemark);
        destinationPlacemark = mapObjects.addPlacemark(point);
        destinationPlacemark.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.marker));
        IconStyle style = new IconStyle();
        style.setScale(0.1f);
        destinationPlacemark.setIconStyle(style);
        destinationPlacemark.setText("Назначение");
        reverseGeocodeForAddress(point, false);
        if (pickupPoint != null) {
            drawRouteLine();
        }
    }

    private void reverseGeocode(Point point) {
        searchManager.submit(point, 18, new SearchOptions(),
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
                        if (response.getCollection().getChildren().isEmpty()) return;
                        etSearch.setText(response.getCollection().getChildren().get(0).getObj().getName());
                    }
                    @Override
                    public void onSearchError(@NonNull Error error) {}
                });
    }

    private void reverseGeocodeForAddress(Point point, boolean isPickup) {
        searchManager.submit(point, 18, new SearchOptions(),
                new Session.SearchListener() {
                    @Override
                    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
                        if (response.getCollection().getChildren().isEmpty()) return;
                        String address = response.getCollection().getChildren().get(0).getObj().getName();
                        if (isPickup) {
                            pickupAddress = address;
                            pickupPlacemark.setText(address);
                        } else {
                            destinationAddress = address;
                            destinationPlacemark.setText(address);
                        }
                    }
                    @Override
                    public void onSearchError(@NonNull Error error) {}
                });
    }

    private void submitSearch(String query) {
        searchSession = searchManager.submit(query,
                VisibleRegionUtils.toPolygon(mapView.getMap().getVisibleRegion()),
                new SearchOptions(), this);
    }

    @Override
    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
        if (response.getCollection().getChildren().isEmpty()) {
            Toast.makeText(getContext(), "Ничего не найдено", Toast.LENGTH_SHORT).show();
            return;
        }
        Point resultPoint = response.getCollection().getChildren().get(0).getObj().getGeometry().get(0).getPoint();
        if (resultPoint != null) {
            mapView.getMap().move(new CameraPosition(resultPoint, 14.0f, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 1), null);
            if (searchModeFrom) {
                setPickupPoint(resultPoint);
            } else {
                setDestinationPoint(resultPoint);
            }
        }
    }

    @Override
    public void onSearchError(@NonNull Error error) {
        Toast.makeText(getContext(), "Ошибка поиска", Toast.LENGTH_SHORT).show();
    }

    private void drawRouteLine() {
        if (currentRoutePolyline != null) mapObjects.remove(currentRoutePolyline);
        if (pickupPoint == null || destinationPoint == null) return;
        List<Point> points = new ArrayList<>();
        points.add(pickupPoint);
        points.add(destinationPoint);
        com.yandex.mapkit.geometry.Polyline polyline = new com.yandex.mapkit.geometry.Polyline(points);
        currentRoutePolyline = mapObjects.addPolyline(polyline);
        currentRoutePolyline.setStrokeColor(Color.BLUE);
        currentRoutePolyline.setStrokeWidth(5.0f);
    }

    private double haversineDistance(Point p1, Point p2) {
        final int R = 6371;
        double lat1 = Math.toRadians(p1.getLatitude());
        double lon1 = Math.toRadians(p1.getLongitude());
        double lat2 = Math.toRadians(p2.getLatitude());
        double lon2 = Math.toRadians(p2.getLongitude());
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private int calculatePrice(double distanceKm) {
        int basePrice = 100;
        int pricePerKm = 30;
        return basePrice + (int)(distanceKm * pricePerKm);
    }

    private void loadAvailableDrivers() {
        driversListener = db.collection("drivers_available")
                .whereEqualTo("isAvailable", true)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    for (PlacemarkMapObject pm : driverPlacemarks) {
                        mapObjects.remove(pm);
                    }
                    driverPlacemarks.clear();
                    for (var doc : value.getDocuments()) {
                        GeoPoint loc = doc.getGeoPoint("location");
                        if (loc != null) {
                            Point point = new Point(loc.getLatitude(), loc.getLongitude());
                            PlacemarkMapObject placemark = mapObjects.addPlacemark(point);
                            placemark.setIcon(ImageProvider.fromResource(requireContext(), R.drawable.driver_marker));
                            IconStyle driverStyle = new IconStyle();
                            driverStyle.setScale(0.1f);
                            placemark.setIconStyle(driverStyle);
                            String driverName = doc.getString("name");
                            placemark.setText("🚖 " + (driverName != null ? driverName : "Водитель"));
                            placemark.addTapListener((mapObject, tapPoint) -> {
                                startActivity(new Intent(getActivity(), DriverProfileViewActivity.class)
                                        .putExtra("driverId", doc.getId()));
                                return true;
                            });
                            driverPlacemarks.add(placemark);
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        if (driversListener != null) driversListener.remove();
        super.onStop();
    }
}