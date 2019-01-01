package com.mathilde.pokekit.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mathilde.pokekit.HandleFileUpload
import com.mathilde.pokekit.R
import com.mathilde.pokekit.model.Pokemon
import kotlinx.android.synthetic.main.item_row.view.*

/**
 * @author mathilde
 * @version 01/01/2019
 */
class PokemonAdapter(private var pokelist: List<Pokemon>, private val handleFileUpload : HandleFileUpload) : RecyclerView.Adapter<PokemonAdapter.PokeHolder>() {

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokeHolder {
        context = parent.context
        val view = LayoutInflater.from(this.context).inflate(R.layout.item_row, parent, false)
        return PokeHolder(view)
    }

    override fun getItemCount() = pokelist.size

    override fun onBindViewHolder(holder: PokeHolder, position: Int) {
        val item = pokelist[position]

        when {
            item.accuracy > .70 -> holder.itemView.itemAccuracy.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            item.accuracy < .30 -> holder.itemView.itemAccuracy.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            else -> holder.itemView.itemAccuracy.setTextColor(ContextCompat.getColor(context,android. R.color.holo_orange_dark))
        }

        with(holder.itemView) {
            itemName.text = item.name
            itemAccuracy.text = "Probability : ${(item.accuracy * 100).toInt()}%"
        }
    }

    fun setList(pokelist: List<Pokemon>) {
        this.pokelist = pokelist
        notifyDataSetChanged()
    }

    private fun showPokemonSpinner() {

    }

    class PokeHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}