package com.irfankhoirul.recipe.modul.ingredient;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.irfankhoirul.recipe.R;
import com.irfankhoirul.recipe.data.pojo.Ingredient;
import com.irfankhoirul.recipe.util.DisplayMetricUtils;
import com.irfankhoirul.recipe.util.RecyclerViewMarginDecoration;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class IngredientFragment extends Fragment {

    @BindView(R.id.rv_ingredient)
    RecyclerView rvIngredient;

    private IngredientAdapter ingredientAdapter;

    public IngredientFragment() {
        // Required empty public constructor
    }

    public static IngredientFragment newInstance(ArrayList<Ingredient> ingredients) {
        IngredientFragment ingredientFragment = new IngredientFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("ingredients", ingredients);
        ingredientFragment.setArguments(bundle);

        return ingredientFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ingredient, container, false);
        ButterKnife.bind(this, view);

        setupRecyclerView();

        return view;
    }

    private void setupRecyclerView() {
        int column = 1;
        int marginInPixel = DisplayMetricUtils.convertDpToPixel(8);

        RecyclerViewMarginDecoration decoration =
                new RecyclerViewMarginDecoration(RecyclerViewMarginDecoration.ORIENTATION_VERTICAL,
                        marginInPixel, column);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        rvIngredient.setLayoutManager(layoutManager);
        rvIngredient.addItemDecoration(decoration);
        ArrayList<Ingredient> ingredients = getArguments().getParcelableArrayList("ingredients");
        ingredientAdapter = new IngredientAdapter(ingredients);
        rvIngredient.setAdapter(ingredientAdapter);
    }

}