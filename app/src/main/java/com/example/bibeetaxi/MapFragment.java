package com.example.bibeetaxi;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Geometry;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.VisibleRegionUtils;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.runtime.Error;

public class MapFragment extends Fragment implements Session.SearchListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MapView mapView;
    private EditText etSearch;
    private Button btnSearch, btnCreateRequest;
    private ProgressBar progressBar;
    private SearchManager searchManager;
    private Session searchSession;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        mapView = view.findViewById(R.id.mapview);
        etSearch = view.findViewById(R.id.etSearch);
        btnSearch = view.findViewById(R.id.btnSearch);
        btnCreateRequest = view.findViewById(R.id.btnCreateRequest);
        progressBar = view.findViewById(R.id.progressBar);

        // Сначала скрываем карту, показываем прогресс
        mapView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        // Откладываем инициализацию карты на 100 мс, чтобы UI успел отрисоваться
        handler.postDelayed(() -> {
            if (!isAdded()) return;
            MapKitFactory.getInstance().onStart();
            mapView.onStart();
            mapView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            // Начальная позиция
            mapView.getMap().move(
                    new CameraPosition(new Point(55.751244, 37.618423), 11.0f, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 0),
                    null);

            // Поиск
            searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);

            // Разрешение геолокации
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }

        }, 100);

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty() && searchManager != null) {
                submitSearch(query);
            }
        });

        btnCreateRequest.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateRideRequestActivity.class));
        });

        return view;
    }

    private void submitSearch(String query) {
        Geometry geometry = VisibleRegionUtils.toPolygon(mapView.getMap().getVisibleRegion());
        searchSession = searchManager.submit(
                query,
                geometry,
                new SearchOptions(),
                this
        );
    }

    @Override
    public void onSearchResponse(@NonNull com.yandex.mapkit.search.Response response) {
        if (response.getCollection().getChildren().isEmpty()) {
            Toast.makeText(getContext(), "Адрес не найден", Toast.LENGTH_SHORT).show();
            return;
        }
        Point resultPoint = response.getCollection().getChildren().get(0).getObj().getGeometry().get(0).getPoint();
        if (resultPoint != null) {
            mapView.getMap().move(
                    new CameraPosition(resultPoint, 14.0f, 0.0f, 0.0f),
                    new Animation(Animation.Type.SMOOTH, 1),
                    null);
        }
    }

    @Override
    public void onSearchError(@NonNull Error error) {
        Toast.makeText(getContext(), "Ошибка поиска", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mapView != null && mapView.getVisibility() == View.VISIBLE) {
            MapKitFactory.getInstance().onStart();
            mapView.onStart();
        }
    }

    @Override
    public void onStop() {
        if (mapView != null) {
            mapView.onStop();
            MapKitFactory.getInstance().onStop();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}