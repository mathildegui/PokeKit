package com.mathilde.pokekit.adapter

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.mathilde.pokekit.HandleFileUpload
import com.mathilde.pokekit.R
import com.mathilde.pokekit.model.Pokemon
import com.mathilde.pokekit.ui.main.pokeArray
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
            btnYes.setOnClickListener {
//                if (isUserSignedIn())
                    handleFileUpload.uploadImageToStorage(item.name)
//                else
//                    startAuth(handleFileUpload as AppCompatActivity)
            }
            btnNo.setOnClickListener {
                showPokemonSpinner()
            }
        }
    }

    fun setList(pokelist: List<Pokemon>) {
        this.pokelist = pokelist
        notifyDataSetChanged()
    }

    private fun showPokemonSpinner() {
        val pokeSpinnerAdapter = ArrayAdapter(context,
                android.R.layout.simple_spinner_item, pokeArray)

        pokeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val view = LayoutInflater.from(context).inflate(R.layout.poke_spinner_dialog, null, false)
        val spinner = view.findViewById<Spinner>(R.id.spinner)

        spinner.adapter = pokeSpinnerAdapter

        val dialog = AlertDialog.Builder(context)
                .setTitle("Help us in making the app better")
                .setMessage("Select correct pokemon from the list below")
                .setView(view)
                .setPositiveButton("Submit") { dialog, _ ->
                    handleFileUpload.uploadImageToStorage(spinner.selectedItem as String)
                    dialog.cancel()
                }
                .create()
        dialog.show()
    }

    class PokeHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}